package com.resonance.zhiji.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        CardEntity::class,
        CardFtsEntity::class,
        CardDraftEntity::class,
        AttachmentEntity::class,
        TagEntity::class,
        CardTagRelationEntity::class,
        MemoryItemEntity::class,
        MemorySourceEntity::class,
        KnowledgeLinkEntity::class,
        GoalEntity::class,
        TaskEntity::class,
        ReminderEntity::class,
        ActionLogEntity::class,
        PostcardRecommendationEntity::class,
        ProviderProfileEntity::class,
        ModelPackageEntity::class,
        ConsentRecordEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun resonanceDao(): ResonanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "resonance_zhiji_v2.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
