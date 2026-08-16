package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.logkeeper.LogKeeperManager
import com.example.data.logkeeper.LogTag
import com.example.data.model.NoteColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [NoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VoiceNotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: VoiceNotesDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): VoiceNotesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoiceNotesDatabase::class.java,
                    "voice_notes_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                LogKeeperManager.log(LogTag.Storage, "VoiceNotesDatabase created. Populating initial seed notes...")
                INSTANCE?.let { database ->
                    scope.launch {
                        populateInitialNotes(database.noteDao())
                    }
                }
            }
        }

        private suspend fun populateInitialNotes(noteDao: NoteDao) {
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
            LogKeeperManager.log(LogTag.Storage, "Initial seed: ${starterNotes.size} notes stored in Room database.")
        }
    }
}
