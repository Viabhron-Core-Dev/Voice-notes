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
    entities = [NoteEntity::class, ModelInfoEntity::class],
    version = 4,
    exportSchema = false
)
abstract class VoiceNotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun modelDao(): ModelDao

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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
