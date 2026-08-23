package com.example.ui.editor

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AndroidSpeechRecognizerEngine
import com.example.audio.AudioCaptureState
import com.example.audio.RawAudioCaptureEngine
import com.example.audio.whisper.InferenceBenchmarkTracker
import com.example.audio.whisper.WhisperInferenceEngine
import com.example.data.db.NoteEntity
import com.example.data.db.VoiceNotesDatabase
import com.example.data.logkeeper.LogKeeperManager
import com.example.data.logkeeper.LogTag
import com.example.data.model.NoteColor
import com.example.data.repository.NotesRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SpeechRecognitionStatus {
    IDLE_SILENCE,       // Grey - no loud audio or silence
    HEARING_SOUND,      // Blue - microphone picking up sound/speech
    WORDS_RECOGNIZED,   // Green - model successfully transcribed words
    NO_WORDS_DETECTED   // Red / Soft Red - sound detected but no recognizable words
}

data class NoteEditorUiState(
    val noteId: Long? = null,
    val title: String = "",
    val contentValue: TextFieldValue = TextFieldValue(""),
    val color: NoteColor = NoteColor.YELLOW,
    val isPinned: Boolean = false,
    val isLoaded: Boolean = false,
    val hasAudio: Boolean = false,
    val audioPath: String? = null,
    val audioDurationMs: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis(),
    val isSavedStatus: Boolean = true,
    val speechStatus: SpeechRecognitionStatus = SpeechRecognitionStatus.IDLE_SILENCE,
    val lastRecognizedSnippet: String = "",
    val userMessage: String? = null
) {
    val contentText: String
        get() = contentValue.text
}

@OptIn(FlowPreview::class)
class NoteEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val database = VoiceNotesDatabase.getDatabase(application, viewModelScope)
    private val repository = NotesRepository(database.noteDao())
    private val modelDao = database.modelDao()
    private val audioCaptureEngine = RawAudioCaptureEngine(application, viewModelScope)
    private val speechRecognizerEngine = AndroidSpeechRecognizerEngine(application)
    private val whisperInferenceEngine = WhisperInferenceEngine(application)

    val captureState: StateFlow<AudioCaptureState> = audioCaptureEngine.captureState
    val currentAmplitude: StateFlow<Float> = audioCaptureEngine.currentAmplitude
    val benchmarkStats = InferenceBenchmarkTracker.stats

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private var initialNoteState: NoteEntity? = null
    private var autoSaveJob: Job? = null

    init {
        // Monitor live amplitude to switch between IDLE_SILENCE (grey) and HEARING_SOUND (blue)
        viewModelScope.launch {
            audioCaptureEngine.currentAmplitude.collect { amplitude ->
                if (_uiState.value.speechStatus != SpeechRecognitionStatus.WORDS_RECOGNIZED) {
                    if (amplitude > 0.08f) {
                        _uiState.update { it.copy(speechStatus = SpeechRecognitionStatus.HEARING_SOUND) }
                    } else {
                        _uiState.update { it.copy(speechStatus = SpeechRecognitionStatus.IDLE_SILENCE) }
                    }
                }
            }
        }

        // Collect exact recognized words from the speech recognizer engine
        viewModelScope.launch {
            speechRecognizerEngine.recognizedTextFlow.collect { text ->
                if (text.isNotBlank()) {
                    _uiState.update {
                        it.copy(
                            speechStatus = SpeechRecognitionStatus.WORDS_RECOGNIZED,
                            lastRecognizedSnippet = text.trim()
                        )
                    }
                    appendTranscribedText(text)

                    kotlinx.coroutines.delay(1800L)
                    _uiState.update {
                        it.copy(
                            speechStatus = if (currentAmplitude.value > 0.08f) SpeechRecognitionStatus.HEARING_SOUND else SpeechRecognitionStatus.IDLE_SILENCE
                        )
                    }
                }
            }
        }

        // Collect emitted audio chunks and run offline Whisper inference pipeline
        viewModelScope.launch {
            audioCaptureEngine.audioChunks.collect { chunk ->
                val activeModel = modelDao.getActiveModel().firstOrNull()
                if (activeModel != null) {
                    whisperInferenceEngine.loadModel(activeModel)
                }

                val transcriptionResult = whisperInferenceEngine.transcribeChunk(chunk)
                if (transcriptionResult.text.isNotBlank()) {
                    _uiState.update {
                        it.copy(
                            speechStatus = SpeechRecognitionStatus.WORDS_RECOGNIZED,
                            lastRecognizedSnippet = transcriptionResult.text.trim()
                        )
                    }
                    appendTranscribedText(transcriptionResult.text)

                    // Hold the green recognized state for 1.8s so the user clearly sees confirmation
                    kotlinx.coroutines.delay(1800L)
                    _uiState.update {
                        it.copy(
                            speechStatus = if (currentAmplitude.value > 0.08f) SpeechRecognitionStatus.HEARING_SOUND else SpeechRecognitionStatus.IDLE_SILENCE
                        )
                    }
                } else if (currentAmplitude.value > 0.15f) {
                    // Audio was loud enough but no words were deciphered
                    _uiState.update { it.copy(speechStatus = SpeechRecognitionStatus.NO_WORDS_DETECTED) }
                    kotlinx.coroutines.delay(1000L)
                    _uiState.update {
                        it.copy(
                            speechStatus = if (currentAmplitude.value > 0.08f) SpeechRecognitionStatus.HEARING_SOUND else SpeechRecognitionStatus.IDLE_SILENCE
                        )
                    }
                }
            }
        }

        // Automatic background auto-save debounce (800ms after last typing/dictation event)
        viewModelScope.launch {
            _uiState
                .debounce(800L)
                .distinctUntilChanged { old, new ->
                    old.title == new.title &&
                    old.contentText == new.contentText &&
                    old.color == new.color &&
                    old.isPinned == new.isPinned &&
                    old.isSavedStatus == new.isSavedStatus
                }
                .collect { state ->
                    if (state.isLoaded && !state.isSavedStatus) {
                        performPersist(state)
                    }
                }
        }
    }

    private fun appendTranscribedText(recognizedText: String) {
        _uiState.update { current ->
            val cleanText = recognizedText.trim()
            if (cleanText.isBlank()) return@update current

            val currentValue = current.contentValue
            val text = currentValue.text
            val selection = currentValue.selection

            val newText: String
            val newCursorPos: Int

            if (selection.start >= 0 && selection.end >= 0 && selection.start < text.length) {
                // Insert at active user cursor / selection point
                val prefix = text.substring(0, selection.start)
                val suffix = text.substring(selection.end)
                val needLeadingSpace = prefix.isNotEmpty() && !prefix.endsWith(" ") && !prefix.endsWith("\n")
                val needTrailingSpace = suffix.isNotEmpty() && !suffix.startsWith(" ") && !suffix.startsWith("\n")
                val formattedSegment = (if (needLeadingSpace) " " else "") + cleanText + (if (needTrailingSpace) " " else "")
                newText = prefix + formattedSegment + suffix
                newCursorPos = (prefix + formattedSegment).length
            } else {
                // Append cleanly at the end with appropriate sentence spacing
                val needLeading = if (text.isBlank()) "" else if (text.endsWith("\n") || text.endsWith(" ")) "" else "\n"
                newText = text + needLeading + cleanText
                newCursorPos = newText.length
            }

            current.copy(
                contentValue = TextFieldValue(
                    text = newText,
                    selection = TextRange(newCursorPos)
                ),
                isSavedStatus = false
            )
        }
    }

    fun startVoiceRecording(): Boolean {
        // Automatically move cursor to the end of note content on the last line so dictation appends at the bottom
        _uiState.update { current ->
            val text = current.contentValue.text
            val newText = if (text.isNotBlank() && !text.endsWith("\n")) {
                "$text\n"
            } else {
                text
            }
            current.copy(
                contentValue = TextFieldValue(
                    text = newText,
                    selection = TextRange(newText.length)
                )
            )
        }

        viewModelScope.launch {
            val activeModel = modelDao.getActiveModel().firstOrNull()
            if (activeModel == null) {
                _uiState.update {
                    it.copy(userMessage = "No Whisper model loaded. Please import a model in the Models tab.")
                }
                LogKeeperManager.log(
                    LogTag.VoiceEngine,
                    "Voice recording started without an active model. Prompting user to import model."
                )
            } else {
                whisperInferenceEngine.loadModel(activeModel)
            }
        }

        speechRecognizerEngine.startListening()
        return audioCaptureEngine.startCapture()
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun stopVoiceRecording() {
        speechRecognizerEngine.stopListening()
        audioCaptureEngine.stopCapture()
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerEngine.release()
        audioCaptureEngine.release()
        whisperInferenceEngine.release()
    }

    fun initialize(noteId: Long?, initialColor: NoteColor = NoteColor.YELLOW) {
        val current = _uiState.value
        // If already loaded with the same non-null existing note, avoid redundant reload
        if (current.isLoaded && noteId != null && noteId != 0L && current.noteId == noteId) {
            return
        }

        if (noteId == null || noteId == 0L) {
            // New Note: Reset all editor fields to fresh blank note state
            initialNoteState = null
            _uiState.update {
                it.copy(
                    noteId = null,
                    title = "",
                    contentValue = TextFieldValue(""),
                    color = initialColor,
                    isPinned = false,
                    isLoaded = true,
                    hasAudio = false,
                    audioPath = null,
                    audioDurationMs = 0L,
                    updatedAt = System.currentTimeMillis(),
                    isSavedStatus = true,
                    speechStatus = SpeechRecognitionStatus.IDLE_SILENCE,
                    lastRecognizedSnippet = ""
                )
            }
            LogKeeperManager.log(
                LogTag.UI_Editor,
                "Initialized New Note Editor (Color: ${initialColor.displayName})"
            )
        } else {
            // Load Existing Note
            initialNoteState = null
            _uiState.update {
                it.copy(
                    isLoaded = false,
                    noteId = noteId
                )
            }
            viewModelScope.launch {
                val existing = repository.getNoteById(noteId).firstOrNull()
                if (existing != null) {
                    initialNoteState = existing
                    val noteColor = NoteColor.fromName(existing.colorTheme)

                    _uiState.update {
                        it.copy(
                            noteId = existing.id,
                            title = existing.title,
                            contentValue = TextFieldValue(
                                text = existing.content,
                                selection = TextRange(existing.content.length)
                            ),
                            color = noteColor,
                            isPinned = existing.isPinned,
                            hasAudio = existing.hasAudio,
                            audioPath = existing.audioPath,
                            audioDurationMs = existing.audioDurationMs,
                            updatedAt = existing.updatedAt,
                            isLoaded = true,
                            isSavedStatus = true,
                            speechStatus = SpeechRecognitionStatus.IDLE_SILENCE,
                            lastRecognizedSnippet = ""
                        )
                    }
                    LogKeeperManager.log(
                        LogTag.UI_Editor,
                        "Loaded note #${existing.id} '${existing.title}' into Editor"
                    )
                }
            }
        }
    }

    fun onTitleChanged(newTitle: String) {
        _uiState.update { it.copy(title = newTitle, isSavedStatus = false) }
    }

    fun onContentValueChanged(newValue: TextFieldValue) {
        _uiState.update { it.copy(contentValue = newValue, isSavedStatus = false) }
    }

    fun insertTextAtCursor(prefixText: String, suffixText: String = "") {
        _uiState.update { current ->
            val currentValue = current.contentValue
            val text = currentValue.text
            val selection = currentValue.selection

            val start = selection.min.coerceIn(0, text.length)
            val end = selection.max.coerceIn(0, text.length)

            val selectedText = if (start < end) text.substring(start, end) else ""
            val replacement = prefixText + selectedText + suffixText

            val newText = text.substring(0, start) + replacement + text.substring(end)
            val newCursor = start + replacement.length

            current.copy(
                contentValue = TextFieldValue(
                    text = newText,
                    selection = TextRange(newCursor)
                ),
                isSavedStatus = false
            )
        }
    }

    fun insertFormattedLine(prefix: String) {
        _uiState.update { current ->
            val currentValue = current.contentValue
            val text = currentValue.text
            val selection = currentValue.selection
            val cursor = selection.start.coerceIn(0, text.length)

            // Find beginning of the current line
            val lastNewLine = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0))
            val lineStart = if (lastNewLine == -1) 0 else lastNewLine + 1

            val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
            val newCursor = cursor + prefix.length

            current.copy(
                contentValue = TextFieldValue(
                    text = newText,
                    selection = TextRange(newCursor)
                ),
                isSavedStatus = false
            )
        }
    }

    fun insertTimestamp() {
        val dateStr = java.text.SimpleDateFormat("MMM d, yyyy h:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        insertTextAtCursor("[$dateStr] ")
    }

    fun clearContent() {
        _uiState.update { current ->
            current.copy(
                contentValue = TextFieldValue(""),
                isSavedStatus = false
            )
        }
    }

    fun onColorSelected(newColor: NoteColor) {
        _uiState.update { it.copy(color = newColor, isSavedStatus = false) }
        LogKeeperManager.log(LogTag.UI_Editor, "Editor note color changed to ${newColor.displayName}")
    }

    fun togglePinned() {
        val nextPin = !_uiState.value.isPinned
        _uiState.update { it.copy(isPinned = nextPin, isSavedStatus = false) }
        LogKeeperManager.log(LogTag.UI_Editor, "Editor note pin toggled to: $nextPin")
    }

    private suspend fun performPersist(state: NoteEditorUiState): Boolean {
        val title = state.title.trim()
        val content = state.contentText.trim()

        if (title.isBlank() && content.isBlank()) {
            if (state.noteId != null) {
                repository.deleteNoteById(state.noteId)
            }
            return false
        }

        val effectiveTitle = if (title.isBlank()) {
            if (content.isNotBlank()) {
                val firstMeaningfulLine = content.lines()
                    .map { it.trim().removePrefix("•").removePrefix("-").removePrefix("[ ]").removePrefix("[x]").trim() }
                    .firstOrNull { it.isNotBlank() }
                    ?.take(36) ?: "Untitled Note"
                firstMeaningfulLine
            } else {
                "Untitled Note"
            }
        } else {
            title
        }

        val now = System.currentTimeMillis()
        val entity = NoteEntity(
            id = state.noteId ?: 0L,
            title = effectiveTitle,
            content = content,
            colorTheme = state.color.name,
            isPinned = state.isPinned,
            isChecklist = false,
            isArchived = false,
            hasAudio = false,
            audioPath = null,
            audioDurationMs = 0L,
            createdAt = initialNoteState?.createdAt ?: now,
            updatedAt = now
        )

        if (state.noteId == null || state.noteId == 0L) {
            val newId = repository.insertNote(entity)
            _uiState.update { it.copy(noteId = newId, updatedAt = now, isSavedStatus = true) }
        } else {
            repository.updateNote(entity)
            _uiState.update { it.copy(updatedAt = now, isSavedStatus = true) }
        }
        return true
    }

    fun saveNote(): Boolean {
        val state = _uiState.value
        viewModelScope.launch {
            performPersist(state)
        }
        return true
    }

    fun deleteCurrentNote() {
        val state = _uiState.value
        state.noteId?.let { id ->
            viewModelScope.launch {
                repository.deleteNoteById(id)
            }
        }
    }
}
