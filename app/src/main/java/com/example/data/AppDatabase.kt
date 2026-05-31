package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "me",
    val username: String,
    val email: String,
    val phone: String,
    val loginMode: String, // GUEST, GOOGLE, FACEBOOK, EMAIL, OTP
    val bio: String,
    val avatarUrl: String,
    val coverPhotoUrl: String,
    val coins: Int = 1000,
    val gems: Int = 50,
    val level: Int = 1,
    val xp: Int = 20,
    val followers: Int = 142,
    val following: Int = 89,
    val creatorRevenue: Double = 0.0,
    val creatorLevel: Int = 1,
    val visitorCount: Int = 540,
    val hasBadgeVoiceMaster: Boolean = true,
    val hasBadgeCryptoGamer: Boolean = false,
    val hasBadgeHighRoller: Boolean = false,
    val fanClubName: String = ""
)

@Entity(tableName = "chat_rooms")
data class RoomEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: String, // "PUBLIC", "PRIVATE", "FAMILY", "MUSIC", "GAMING"
    val description: String,
    val isPasswordProtected: Boolean,
    val hasActivePk: Boolean = false,
    val pkLeftUserScore: Int = 0,
    val pkRightUserScore: Int = 0,
    val activeHostName: String = "System",
    val passwordInput: String = ""
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val roomId: Int = 0, // 0 for Global/Admin, positive for specific Rooms
    val senderName: String,
    val senderLevel: Int = 1,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVoice: Boolean = false,
    val voiceDuration: Int = 0, // seconds
    val isGift: Boolean = false,
    val giftType: String = "", // "Rose", "Heart", etc.
    val isAiModerated: Boolean = false,
    val translatedText: String = ""
)

@Entity(tableName = "community_posts")
data class CommunityPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val postType: String, // "POST", "POLL", "EVENT"
    val pollQuestion: String = "",
    val pollOptionA: String = "",
    val pollOptionB: String = "",
    val votesA: Int = 0,
    val votesB: Int = 0,
    val eventTime: String = "",
    val likesCount: Int = 0
)

@Dao
interface LiveDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUserProfileSync(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM chat_rooms ORDER BY id DESC")
    fun getRooms(): Flow<List<RoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity)

    @Delete
    suspend fun deleteRoom(room: RoomEntity)

    @Query("SELECT * FROM messages WHERE roomId = :roomId ORDER BY id ASC")
    fun getMessagesForRoom(roomId: Int): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM community_posts ORDER BY id DESC")
    fun getPosts(): Flow<List<CommunityPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: CommunityPostEntity)

    @Query("UPDATE community_posts SET votesA = votesA + 1 WHERE id = :postId")
    suspend fun voteA(postId: Int)

    @Query("UPDATE community_posts SET votesB = votesB + 1 WHERE id = :postId")
    suspend fun voteB(postId: Int)

    @Query("UPDATE community_posts SET likesCount = likesCount + 1 WHERE id = :postId")
    suspend fun likePost(postId: Int)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: Int)
}

@Database(
    entities = [UserProfileEntity::class, RoomEntity::class, MessageEntity::class, CommunityPostEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun liveDao(): LiveDao
}
