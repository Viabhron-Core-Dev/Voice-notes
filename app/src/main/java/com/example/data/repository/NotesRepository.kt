package com.example.data.repository

import com.example.data.db.NoteDao
import com.example.data.db.NoteEntity
import com.example.data.logkeeper.LogKeeperManager
import com.example.data.logkeeper.LogTag
import com.example.data.model.NoteColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class NotesRepository(
    private val noteDao: NoteDao
) {
    val activeNotes: Flow<List<NoteEntity>> = noteDao.getActiveNotes()
        .onEach { notes ->
            LogKeeperManager.log(LogTag.Storage, "Loaded ${notes.size} active notes from database")
        }

    val archivedNotes: Flow<List<NoteEntity>> = noteDao.getArchivedNotes()

    fun getNoteById(id: Long): Flow<NoteEntity?> = noteDao.getNoteById(id)

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    fun getNotesByColor(color: NoteColor): Flow<List<NoteEntity>> = noteDao.getNotesByColor(color.name)

    suspend fun insertNote(note: NoteEntity): Long {
        val id = noteDao.insertNote(note)
        LogKeeperManager.log(LogTag.Storage, "Note created #$id: '${note.title}' (${note.colorTheme})")
        return id
    }

    suspend fun updateNote(note: NoteEntity) {
        noteDao.updateNote(note)
        LogKeeperManager.log(LogTag.Storage, "Note updated #${note.id}: '${note.title}'")
    }

    suspend fun deleteNote(note: NoteEntity) {
        noteDao.deleteNote(note)
        LogKeeperManager.log(LogTag.Storage, "Note deleted #${note.id}: '${note.title}'")
    }

    suspend fun deleteNoteById(id: Long) {
        noteDao.deleteNoteById(id)
        LogKeeperManager.log(LogTag.Storage, "Note deleted #$id")
    }

    suspend fun togglePin(id: Long, currentPinState: Boolean) {
        val nextState = !currentPinState
        noteDao.updatePinStatus(id, nextState)
        LogKeeperManager.log(LogTag.Storage, "Note #$id pin status changed to: $nextState")
    }

    suspend fun toggleArchive(id: Long, currentArchiveState: Boolean) {
        val nextState = !currentArchiveState
        noteDao.updateArchiveStatus(id, nextState)
        LogKeeperManager.log(LogTag.Storage, "Note #$id archive status changed to: $nextState")
    }

    suspend fun updateNoteColor(id: Long, color: NoteColor) {
        noteDao.updateNoteColor(id, color.name)
        LogKeeperManager.log(LogTag.Storage, "Note #$id color updated to ${color.displayName}")
    }

    suspend fun addQuickNote(
        title: String,
        content: String = "",
        color: NoteColor = NoteColor.YELLOW,
        isChecklist: Boolean = false
    ): Long {
        val note = NoteEntity(
            title = title,
            content = content,
            colorTheme = color.name,
            isChecklist = isChecklist
        )
        return insertNote(note)
    }

    suspend fun ensureStarterNotesIfEmpty() {
        val count = noteDao.getActiveNoteCount()
        if (count == 0) {
            LogKeeperManager.log(LogTag.Storage, "No notes found in Room DB. Populating default seed notes...")
            val starterNotes = listOf(
                NoteEntity(
                    title = "To do",
                    content = "Buy groceries\nReview UI changes\nCheck Log Keeper",
                    colorTheme = NoteColor.PINK.name,
                    isPinned = true,
                    isChecklist = true,
                    createdAt = System.currentTimeMillis() - 3600000,
                    updatedAt = System.currentTimeMillis() - 3600000
                ),
                NoteEntity(
                    title = "Record of things bought",
                    content = "Microphone adapter\nStylus pen\nCoffee beans",
                    colorTheme = NoteColor.PINK.name,
                    isPinned = false,
                    isChecklist = false,
                    createdAt = System.currentTimeMillis() - 86400000,
                    updatedAt = System.currentTimeMillis() - 86400000
                ),
                NoteEntity(
                    title = "Good prompts",
                    content = "System instructions for Android voice offline transcriber",
                    colorTheme = NoteColor.PEACH.name,
                    isPinned = true,
                    isChecklist = true,
                    createdAt = System.currentTimeMillis() - 7200000,
                    updatedAt = System.currentTimeMillis() - 7200000
                ),
                NoteEntity(
                    title = "Omnivian",
                    content = "Dual engine cross-platform architecture concept",
                    colorTheme = NoteColor.YELLOW.name,
                    isPinned = false,
                    isChecklist = false,
                    createdAt = System.currentTimeMillis() - 14400000,
                    updatedAt = System.currentTimeMillis() - 14400000
                ),
                NoteEntity(
                    title = "A web novel ai writer",
                    content = "Offline narrative generation draft with local persistence",
                    colorTheme = NoteColor.YELLOW.name,
                    isPinned = false,
                    isChecklist = false,
                    createdAt = System.currentTimeMillis() - 172800000,
                    updatedAt = System.currentTimeMillis() - 172800000
                ),
                NoteEntity(
                    title = "Voice Notes offline engine",
                    content = "Room SQLite Database layer initialized for fast 60fps scrolling and offline persistence.",
                    colorTheme = NoteColor.BLUE.name,
                    isPinned = false,
                    isChecklist = false,
                    createdAt = System.currentTimeMillis() - 1800000,
                    updatedAt = System.currentTimeMillis() - 1800000
                ),
                NoteEntity(
                    title = "Meeting agenda - Board",
                    content = "1. Financial review\n2. Q3 Roadmap release\n3. Mobile app deploy",
                    colorTheme = NoteColor.GREEN.name,
                    isPinned = false,
                    isChecklist = false,
                    createdAt = System.currentTimeMillis() - 900000,
                    updatedAt = System.currentTimeMillis() - 900000
                )
            )
            noteDao.insertNotes(starterNotes)
            LogKeeperManager.log(LogTag.Storage, "Inserted ${starterNotes.size} starter notes into Room database.")
        }
    }
}
