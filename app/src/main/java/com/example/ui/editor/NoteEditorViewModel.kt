package com.example.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioCaptureState
import com.example.audio.RawAudioCaptureEngine
import com.example.audio.whisper.WhisperInferenceEngine
import com.example.data.db.NoteEntity
import com.example.data.db.VoiceNotesDatabase
import com.example.data.logkeeper.LogKeeperManager
import com.example.data.logkeeper.LogTag
import com.example.data.model.NoteColor
import com.example.data.repository.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteEditorUiState(
    val noteId: Long? = null,
    val title: String = "",
    val content: String = "",
    val color: NoteColor = NoteColor.YELLOW,
    val isPinned: Boolean = false,
    val isLoaded: Boolean = false,
    val hasAudio: Boolean = false,
    val audioPath: String? = null,
    val audioDurationMs: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis(),
    val isSavedStatus: Boolean = true
)

class NoteEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val database = VoiceNotesDatabase.getDatabase(application, viewModelScope)
    private val repository = NotesRepository(database.noteDao())
    private val modelDao = database.modelDao()
    private val audioCaptureEngine = RawAudioCaptureEngine(application, viewModelScope)
    private val whisperInferenceEngine = WhisperInferenceEngine(application)

    val captureState: StateFlow<AudioCaptureState> = audioCaptureEngine.captureState
    val currentAmplitude: StateFlow<Float> = audioCaptureEngine.currentAmplitude
    val benchmarkStats = com.example.audio.whisper.InferenceBenchmarkTracker.stats

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private var initialNoteState: NoteEntity? = null

    init {
        // Collect emitted audio chunks and run offline Whisper inference pipeline
        viewModelScope.launch {
            audioCaptureEngine.audioChunks.collect { chunk ->
                val activeModel = modelDao.getActiveModel().firstOrNull()
                if (activeModel != null) {
                    whisperInferenceEngine.loadModel(activeModel)
                }

                val transcriptionResult = whisperInferenceEngine.transcribeChunk(chunk)
                if (transcriptionResult.text.isNotBlank()) {
                    _uiState.update { current ->
                        val separator = if (current.content.isBlank()) "" else "\n"
                        current.copy(
                            content = current.content + separator + transcriptionResult.text,
                            isSavedStatus = false
                        )
                    }
                }
            }
        }
    }

    fun startVoiceRecording(): Boolean {
        return audioCaptureEngine.startCapture()
    }

    fun stopVoiceRecording() {
        audioCaptureEngine.stopCapture()
    }

    override fun onCleared() {
        super.onCleared()
        audioCaptureEngine.release()
        whisperInferenceEngine.release()
    }

    fun initialize(noteId: Long?, initialColor: NoteColor = NoteColor.YELLOW) {
        if (_uiState.value.isLoaded) return

        if (noteId == null || noteId == 0L) {
            // New Note
            _uiState.update {
                it.copy(
                    noteId = null,
                    title = "",
                    content = "",
                    color = initialColor,
                    isPinned = false,
                    isLoaded = true,
                    updatedAt = System.currentTimeMillis(),
                    isSavedStatus = true
                )
            }
            LogKeeperManager.log(
                LogTag.UI_Editor,
                "Initialized New Note Editor (Color: ${initialColor.displayName})"
            )
        } else {
            // Load Existing Note
            viewModelScope.launch {
                val existing = repository.getNoteById(noteId).firstOrNull()
                if (existing != null) {
                    initialNoteState = existing
                    val noteColor = NoteColor.fromName(existing.colorTheme)

                    _uiState.update {
                        it.copy(
                            noteId = existing.id,
                            title = existing.title,
                            content = existing.content,
                            color = noteColor,
                            isPinned = existing.isPinned,
                            hasAudio = existing.hasAudio,
                            audioPath = existing.audioPath,
                            audioDurationMs = existing.audioDurationMs,
                            updatedAt = existing.updatedAt,
                            isLoaded = true,
                            isSavedStatus = true
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

    fun onContentChanged(newContent: String) {
        _uiState.update { it.copy(content = newContent, isSavedStatus = false) }
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

    fun saveNote(): Boolean {
        val state = _uiState.value
        val title = state.title.trim()
        val content = state.content.trim()

        // If completely empty and new, do not save an empty ghost record
        if (title.isBlank() && content.isBlank()) {
            if (state.noteId != null) {
                // If existing note was emptied completely, we delete it
                viewModelScope.launch {
                    repository.deleteNoteById(state.noteId)
                }
            }
            return false
        }

        val effectiveTitle = if (title.isBlank()) {
            if (content.isNotBlank()) {
                content.lines().firstOrNull()?.take(30) ?: "Untitled Note"
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
            hasAudio = state.hasAudio,
            audioPath = state.audioPath,
            audioDurationMs = state.audioDurationMs,
            createdAt = initialNoteState?.createdAt ?: now,
            updatedAt = now
        )

        viewModelScope.launch {
            if (state.noteId == null || state.noteId == 0L) {
                val newId = repository.insertNote(entity)
                _uiState.update { it.copy(noteId = newId, updatedAt = now, isSavedStatus = true) }
            } else {
                repository.updateNote(entity)
                _uiState.update { it.copy(updatedAt = now, isSavedStatus = true) }
            }
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
