package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiApiClient
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class EchoViewModel(context: Context) : ViewModel() {

    private val db = androidx.room.Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "echolive_vox.db"
    ).fallbackToDestructiveMigration().build()

    private val dao = db.liveDao()

    // --- State Observables ---
    val userProfile: StateFlow<UserProfileEntity?> = dao.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val roomList: StateFlow<List<RoomEntity>> = dao.getRooms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val postsList: StateFlow<List<CommunityPostEntity>> = dao.getPosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Screen / Navigation State ---
    val currentTab = MutableStateFlow("explore") // explore, community, wallet, creator, admin, developer
    val activeRoom = MutableStateFlow<RoomEntity?>(null)
    
    // --- Room Specific States ---
    private val _roomMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val roomMessages: StateFlow<List<MessageEntity>> = _roomMessages.asStateFlow()

    val micQueue = MutableStateFlow<List<String>>(listOf("Alex (LV.4)", "Taylor (LV.12)", "Jordan (LV.8)"))
    val isMicMuted = MutableStateFlow(true)
    val viewerCount = MutableStateFlow(240)
    val liveScores = MutableStateFlow(Pair(12400, 11950)) // Left vs Right PK live scores

    // --- Active Gift Animation Overlay Tracker ---
    val activeGiftAnimation = MutableStateFlow<String?>(null) // "Rose", "Heart", "Crown", etc.

    // --- Direct Message State (Global Hub) ---
    val directMessages = MutableStateFlow<List<MessageEntity>>(emptyList())

    // --- Mini-Games State ---
    // 1. Dice Betting
    val diceNumber = MutableStateFlow(3)
    val diceBetOption = MutableStateFlow("High") // High (4-6), Low (1-3)
    val diceBetAmount = MutableStateFlow(10)
    val diceGameResult = MutableStateFlow("")

    // 2. Quiz Game
    val quizQuestionIndex = MutableStateFlow(0)
    val quizSelectedOption = MutableStateFlow<String?>(null)
    val quizResultState = MutableStateFlow<String?>(null) // "CORRECT!", "WRONG!"
    val quizScore = MutableStateFlow(0)

    val quizQuestions = listOf(
        Quiz("Which virtual gift is the rarest and most expensive on EchoLive?", listOf("Yacht", "Dragon", "Crown", "Gold Rose"), 1),
        Quiz("What is the primary currency used to send virtual gifts?", listOf("Coins", "Gems", "Gold Bars", "Credits"), 0),
        Quiz("EchoLive combines standard voice chat room operations with what backend engine?", listOf("WebSockets", "Firebase Realtime", "REST Gateway", "GraphQL"), 0)
    )

    // 3. Ludo Simulator
    val ludoTokenPosition = MutableStateFlow(0) // Represent path step
    val ludoDiceRoll = MutableStateFlow(1)
    val ludoGameLog = MutableStateFlow("Click roll dice to start your path!")

    // --- AI Companion Prompt Input ---
    val aiCompanionMessage = MutableStateFlow("")
    val aiCompanionReply = MutableStateFlow("AI: Hey there! Ask me to translate chats, host a game, or serve as an automated moderator!")
    val isAiLoading = MutableStateFlow(false)

    init {
        // Run database seed in background
        viewModelScope.launch {
            seedDatabase()
            listenToActiveRoomMessages()
        }
    }

    private suspend fun seedDatabase() {
        val user = dao.getUserProfileSync()
        if (user == null) {
            val sampleUser = UserProfileEntity(
                username = "Guest_" + Random.nextInt(1000, 9999),
                email = "guest@echolive.com",
                phone = "+1 (555) 0192-384",
                loginMode = "GUEST",
                bio = "Vocalist. Gamer. Exploring the galaxy of social voice chats!",
                avatarUrl = "avatar_purple",
                coverPhotoUrl = "default_cover",
                coins = 5000,
                gems = 120,
                level = 3,
                xp = 450,
                followers = 112,
                following = 46,
                creatorRevenue = 412.50,
                creatorLevel = 2,
                hasBadgeVoiceMaster = true,
                hasBadgeCryptoGamer = true
            )
            dao.saveUserProfile(sampleUser)
        }

        // Seed some standard Voice Rooms
        dao.getRooms().first().let { rooms ->
            if (rooms.isEmpty()) {
                val presets = listOf(
                    RoomEntity(title = "🎧 Late Night Acoustic Session", type = "MUSIC", description = "Vibe, play guitar, sing along. Hosted by Aria.", isPasswordProtected = false, activeHostName = "Aria_Music"),
                    RoomEntity(title = "🎮 MLBB & Valorant Grind Center", type = "GAMING", description = "Team matchmaker room. High levels only. Voice required.", isPasswordProtected = false, activeHostName = "ViperGamer"),
                    RoomEntity(title = "🔥 PK Arena - Master Battle!", type = "PUBLIC", description = "Epic live gift battle. Dynamic rankings. Send Dragon to trigger fire cascade!", isPasswordProtected = false, hasActivePk = true, pkLeftUserScore = 14500, pkRightUserScore = 12200, activeHostName = "QueenK"),
                    RoomEntity(title = "🔒 VIP Secret Lounge", type = "PRIVATE", description = "Private meeting space. Enter password to connect.", isPasswordProtected = true, passwordInput = "7777", activeHostName = "HostX")
                )
                presets.forEach { dao.insertRoom(it) }
            }
        }

        // Seed some community posts
        dao.getPosts().first().let { posts ->
            if (posts.isEmpty()) {
                val presetPosts = listOf(
                    CommunityPostEntity(senderName = "Aria_Music", content = "Hey everyone! Tonight at 9 PM UTC, I am hosting the Acoustic Open Mic event. Make sure to reserve your mic queue slots in advance in my room! 🎤🎸", postType = "EVENT", eventTime = "Today, 9:00 PM UTC", likesCount = 42),
                    CommunityPostEntity(senderName = "QueenK", content = "Who is going to support my PK Battle today? The opponent has a massive team! Let's defend our Crown title! ⚔️👑", postType = "POST", likesCount = 18),
                    CommunityPostEntity(senderName = "SystemModerator", content = "Poll: What's your favorite virtual gift in EchoLive?", postType = "POLL", pollQuestion = "Primary Gift Choice", pollOptionA = "Dragon (5000 Coins)", pollOptionB = "Yacht (10000 Coins)", votesA = 312, votesB = 256)
                )
                presetPosts.forEach { dao.insertPost(it) }
            }
        }
    }

    private fun listenToActiveRoomMessages() {
        viewModelScope.launch {
            activeRoom.flatMapLatest { room ->
                if (room != null) {
                    dao.getMessagesForRoom(room.id)
                } else {
                    flowOf(emptyList())
                }
            }.collect { msgList ->
                _roomMessages.value = msgList
            }
        }
    }

    // --- User Registration and Profile login triggers ---
    fun registerOrLogin(mode: String, customUser: String = "", customEmail: String = "", customPhone: String = "") {
        viewModelScope.launch {
            val formattedName = if (customUser.isNotBlank()) customUser else "${mode.lowercase().capitalize()}_User_" + Random.nextInt(100, 999)
            val formattedEmail = if (customEmail.isNotBlank()) customEmail else "${formattedName.lowercase()}@echolive.com"
            val formattedPhone = if (customPhone.isNotBlank()) customPhone else "+1 (555) 001-${Random.nextInt(1000, 9999)}"

            val newProfile = UserProfileEntity(
                username = formattedName,
                email = formattedEmail,
                phone = formattedPhone,
                loginMode = mode,
                bio = "New, active user on EchoLive! Excited for PK and Games.",
                avatarUrl = when(mode) {
                    "GOOGLE" -> "avatar_google"
                    "FACEBOOK" -> "avatar_facebook"
                    "OTP" -> "avatar_phone"
                    else -> "avatar_cyan"
                },
                coverPhotoUrl = "default_cover",
                coins = 3000,
                gems = 60,
                level = 1,
                xp = 0
            )
            dao.saveUserProfile(newProfile)
        }
    }

    fun updateProfile(bioText: String, nameText: String, fanClub: String = "") {
        viewModelScope.launch {
            userProfile.value?.let { current ->
                val updated = current.copy(
                    bio = bioText,
                    username = nameText,
                    fanClubName = fanClub
                )
                dao.saveUserProfile(updated)
            }
        }
    }

    // --- Voice Rooms actions ---
    fun createRoom(title: String, type: String, description: String, isPasswordProtected: Boolean, passVal: String) {
        viewModelScope.launch {
            val user = userProfile.value?.username ?: "Host"
            val newRoom = RoomEntity(
                title = title,
                type = type,
                description = description,
                isPasswordProtected = isPasswordProtected,
                passwordInput = passVal,
                activeHostName = user
            )
            dao.insertRoom(newRoom)
        }
    }

    fun joinRoom(room: RoomEntity) {
        activeRoom.value = room
        // Reset state
        viewerCount.value = Random.nextInt(150, 600)
        liveScores.value = Pair(Random.nextInt(5000, 15000), Random.nextInt(5000, 15000))
        isMicMuted.value = true
        activeGiftAnimation.value = null
        
        // Add a friendly greeting message
        viewModelScope.launch {
            val joinMessage = MessageEntity(
                roomId = room.id,
                senderName = "System",
                content = "${userProfile.value?.username ?: "Guest"} joined the room. Wave hello! 👋",
                senderLevel = 99
            )
            dao.insertMessage(joinMessage)
        }
    }

    fun leaveRoom() {
        activeRoom.value = null
    }

    fun sendMessageToRoom(text: String, isVoice: Boolean = false, voiceSec: Int = 0) {
        val currentRoom = activeRoom.value ?: return
        val profile = userProfile.value ?: return
        viewModelScope.launch {
            val msg = MessageEntity(
                roomId = currentRoom.id,
                senderName = profile.username,
                senderLevel = profile.level,
                content = text,
                isVoice = isVoice,
                voiceDuration = voiceSec
            )
            dao.insertMessage(msg)

            // Dynamic leveling: award 5 XP for sending message
            awardXp(5)

            // Trigger simulated bot reply sometimes or check AI Moderation inside code
            if (text.startsWith("@ai", ignoreCase = true)) {
                callAiRoomCompanion(text, currentRoom.id)
            }
        }
    }

    fun toggleMic() {
        isMicMuted.value = !isMicMuted.value
    }

    fun joinMicQueue() {
        val name = userProfile.value?.username ?: "Guest"
        val level = userProfile.value?.level ?: 1
        val list = micQueue.value.toMutableList()
        val entry = "$name (LV.$level)"
        if (!list.contains(entry)) {
            list.add(entry)
            micQueue.value = list
        }
    }

    fun leaveMicQueue() {
        val name = userProfile.value?.username ?: "Guest"
        val level = userProfile.value?.level ?: 1
        val list = micQueue.value.toMutableList()
        val entry = "$name (LV.$level)"
        list.remove(entry)
        micQueue.value = list
    }

    // --- Economy & Streaming PK battles ---
    fun sendVirtualGift(gift: Gift) {
        val profile = userProfile.value ?: return
        val currentRoom = activeRoom.value ?: return
        if (profile.coins < gift.coinCost) return // Insufficient coins

        viewModelScope.launch {
            // 1. Deduct coins and award gems to room/creator balance
            val updatedProfile = profile.copy(
                coins = profile.coins - gift.coinCost,
                gems = profile.gems + (gift.coinCost / 10), // gems reward (10% of coin cost)
                xp = profile.xp + (gift.coinCost * 2) // earn double XP of coin cost!
            )
            dao.saveUserProfile(updatedProfile)
            
            // Check for profile level up!
            checkLevelUp(updatedProfile)

            // 2. Insert gift notification message
            val giftMsg = MessageEntity(
                roomId = currentRoom.id,
                senderName = profile.username,
                senderLevel = profile.level,
                content = "sent a virtual ${gift.name}! 🎁✨",
                isGift = true,
                giftType = gift.name
            )
            dao.insertMessage(giftMsg)

            // 3. Update PK Score if Active
            if (currentRoom.hasActivePk) {
                val left = liveScores.value.first + gift.coinCost
                liveScores.value = Pair(left, liveScores.value.second)
            }

            // 4. Update Creator Revenue tracking
            val updatedRevenue = updatedProfile.copy(
                creatorRevenue = updatedProfile.creatorRevenue + (gift.coinCost * 0.05) // 5% of coin value to creator revenue wallet
            )
            dao.saveUserProfile(updatedRevenue)

            // 5. Trigger animation
            activeGiftAnimation.value = gift.name
        }
    }

    fun clearGiftAnimation() {
        activeGiftAnimation.value = null
    }

    fun rechargeWallet(amountCoins: Int, priceusd: Double) {
        val profile = userProfile.value ?: return
        viewModelScope.launch {
            val updated = profile.copy(
                coins = profile.coins + amountCoins
            )
            dao.saveUserProfile(updated)
            
            // Log global metrics to messages
            val log = MessageEntity(
                roomId = 0,
                senderName = "System",
                content = "${profile.username} recharged wallet with $amountCoins Coins! 🎉"
            )
            dao.insertMessage(log)
        }
    }

    private suspend fun checkLevelUp(profile: UserProfileEntity) {
        val xpNeeded = profile.level * 1000
        if (profile.xp >= xpNeeded) {
            val nextLvl = profile.level + 1
            val updated = profile.copy(
                level = nextLvl,
                xp = profile.xp - xpNeeded,
                hasBadgeHighRoller = if (nextLvl >= 5) true else profile.hasBadgeHighRoller
            )
            dao.saveUserProfile(updated)

            // Send notification
            val levelUpMsg = MessageEntity(
                roomId = 0,
                senderName = "EchoLive",
                content = "Congratulations to ${profile.username} for reaching Level $nextLvl! 🚀👑"
            )
            dao.insertMessage(levelUpMsg)
        }
    }

    private fun awardXp(amount: Int) {
        val profile = userProfile.value ?: return
        viewModelScope.launch {
            val copy = profile.copy(xp = profile.xp + amount)
            dao.saveUserProfile(copy)
            checkLevelUp(copy)
        }
    }

    // --- Community Posts ---
    fun insertCommunityFeed(text: String, postType: String, q: String = "", opA: String = "", opB: String = "", evtTime: String = "") {
        viewModelScope.launch {
            val provider = userProfile.value?.username ?: "Guest"
            val newFeed = CommunityPostEntity(
                senderName = provider,
                content = text,
                postType = postType,
                pollQuestion = q,
                pollOptionA = opA,
                pollOptionB = opB,
                eventTime = evtTime
            )
            dao.insertPost(newFeed)
        }
    }

    fun votePoll(postId: Int, selectedOption: String) {
        viewModelScope.launch {
            if (selectedOption == "A") {
                dao.voteA(postId)
            } else {
                dao.voteB(postId)
            }
        }
    }

    fun likePost(postId: Int) {
        viewModelScope.launch {
            dao.likePost(postId)
        }
    }

    // --- Mini-Games ---
    // Game 1: Dice High/Low Bet
    fun playDiceBetting() {
        val profile = userProfile.value ?: return
        val bet = diceBetAmount.value
        if (profile.coins < bet) {
            diceGameResult.value = "Insufficient coins!"
            return
        }

        viewModelScope.launch {
            val roll = Random.nextInt(1, 7)
            diceNumber.value = roll

            val isHigh = roll >= 4
            val won = (diceBetOption.value == "High" && isHigh) || (diceBetOption.value == "Low" && !isHigh)

            val updatedCoins = if (won) {
                profile.coins + bet // 1:1 return
            } else {
                profile.coins - bet
            }

            val newProfile = profile.copy(coins = updatedCoins)
            dao.saveUserProfile(newProfile)

            diceGameResult.value = if (won) {
                "YOU WON! Dice was $roll. Earned $bet Coins! 🎉"
            } else {
                "YOU LOST. Dice was $roll. Lost $bet Coins. 😢"
            }
            awardXp(5)
        }
    }

    // Game 2: Quiz Show
    fun answerQuizQuestion(selectedIdx: Int) {
        val currentIdx = quizQuestionIndex.value
        val correctIdx = quizQuestions[currentIdx].correctIndex
        quizSelectedOption.value = quizQuestions[currentIdx].options[selectedIdx]

        if (selectedIdx == correctIdx) {
            quizResultState.value = "CORRECT!"
            quizScore.value = quizScore.value + 1
            // Award coins for correct answer
            viewModelScope.launch {
                userProfile.value?.let { profile ->
                    dao.saveUserProfile(profile.copy(coins = profile.coins + 50))
                }
            }
        } else {
            quizResultState.value = "WRONG! Correct was: ${quizQuestions[currentIdx].options[correctIdx]}"
        }
    }

    fun nextQuizQuestion() {
        val currentIdx = quizQuestionIndex.value
        if (currentIdx < quizQuestions.size - 1) {
            quizQuestionIndex.value = currentIdx + 1
            quizSelectedOption.value = null
            quizResultState.value = null
        } else {
            // Reset Quiz score
            quizQuestionIndex.value = 0
            quizSelectedOption.value = null
            quizResultState.value = "Finished! Score: ${quizScore.value}/3. Play again anytime."
            quizScore.value = 0
        }
    }

    // Game 3: Ludo path track roll
    fun rollLudoDice() {
        val roll = Random.nextInt(1, 7)
        ludoDiceRoll.value = roll
        val currentPos = ludoTokenPosition.value
        val nextPos = (currentPos + roll).coerceAtMost(24) // 24 steps track
        ludoTokenPosition.value = nextPos
        
        if (nextPos == 24) {
            ludoGameLog.value = "🎲 Rolled $roll. VICTORY! You completed the Ludo track! Awarded 200 Coins! ✨"
            ludoTokenPosition.value = 0 // Reset
            viewModelScope.launch {
                userProfile.value?.let { profile ->
                    dao.saveUserProfile(profile.copy(coins = profile.coins + 200))
                }
            }
        } else {
            ludoGameLog.value = "🎲 Rolled $roll. Advanced to cell $nextPos / 24."
        }
    }

    // --- AI Companion and Mod/Translation using Gemini ---
    fun callAiCompanion() {
        val text = aiCompanionMessage.value
        if (text.isBlank()) return
        aiCompanionMessage.value = ""
        isAiLoading.value = true
        aiCompanionReply.value = "AI is thinking..."

        viewModelScope.launch {
            val systemPrompt = "You are EchoAssist, a friendly AI moderator and community builder in the EchoLive audio social streaming app. Help the user learn about streaming, PK Battles, or virtual gifts. Keep responses polite, concise (max 2 sentences), and encouraging of digital companionship."
            val reply = GeminiApiClient.callGemini(text, systemPrompt)
            aiCompanionReply.value = "AI: $reply"
            isAiLoading.value = false
        }
    }

    private fun callAiRoomCompanion(prompt: String, rmId: Int) {
        viewModelScope.launch {
            val cleanPrompt = prompt.removePrefix("@ai").removePrefix("@AI").trim()
            val systemPrompt = "You are RoomChatAI, a dynamic AI conversational guest within an EchoLive voice chat room. Answer queries in a casual, lively voice with helpful and chatty formatting. Max 2 short sentences."
            val aiResponse = GeminiApiClient.callGemini(cleanPrompt, systemPrompt)
            val replyMsg = MessageEntity(
                roomId = rmId,
                senderName = "✨ AI Guest",
                senderLevel = 10,
                content = aiResponse
            )
            dao.insertMessage(replyMsg)
        }
    }

    fun translateMessage(msgId: Int, currentContent: String, language: String) {
        viewModelScope.launch {
            // Optimistically show processing state
            val translationPrompt = "Translate the following chat message to $language. Provide only the direct translation text without metadata or extra text: \"$currentContent\""
            val systemPrompt = "You are the specialized AI Translator inside the EchoLive voice platform. Act accurately."
            val translated = GeminiApiClient.callGemini(translationPrompt, systemPrompt)
            
            // Logically update the messages list or DMs
            val updatedMessages = _roomMessages.value.map { msg ->
                if (msg.id == msgId) {
                    msg.copy(translatedText = "[$language: $translated]")
                } else msg
            }
            _roomMessages.value = updatedMessages
        }
    }

    fun moderateChatMessage(msg: MessageEntity) {
        viewModelScope.launch {
            val systemPrompt = "You are an automated safety filter. Evaluate the message. Redact or edit only if it contains extreme spam, slurs, or highly abusive text. Return 'SAFE' or return the redacted message text directly inside brackets."
            val moderationResult = GeminiApiClient.callGemini(msg.content, systemPrompt)
            if (moderationResult.trim().uppercase() != "SAFE" && !moderationResult.contains("Network error")) {
                val updatedMessages = _roomMessages.value.map { item ->
                    if (item.id == msg.id) {
                        item.copy(content = "🚫 [Inappropriate content warning]: " + msg.content, isAiModerated = true)
                    } else item
                }
                _roomMessages.value = updatedMessages
            }
        }
    }

    // --- Admin Dashboard logic ---
    fun deleteMessageAdmin(msgId: Int) {
        viewModelScope.launch {
            dao.deleteMessage(msgId)
        }
    }

    fun resetProfileMetrics() {
        viewModelScope.launch {
            userProfile.value?.let { current ->
                val updated = current.copy(
                    coins = 5000,
                    gems = 100,
                    level = 1,
                    xp = 20,
                    creatorRevenue = 0.0
                )
                dao.saveUserProfile(updated)
            }
        }
    }
}

data class Quiz(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

data class Gift(
    val name: String,
    val icon: String,
    val coinCost: Int,
    val description: String
)

val GiffShopList = listOf(
    Gift("Rose", "🌹", 10, "Simple classic token of love"),
    Gift("Heart", "❤️", 50, "Send dynamic floating heart bubbles"),
    Gift("Crown", "👑", 1000, "Gold and royal majestic crown dropping with sparkles"),
    Gift("Supercar", "🏎️", 2500, "Zoom across the screen with neon red smoke trails"),
    Gift("Castle", "🏰", 5000, "Construct a glowing majestic space cyber castle"),
    Gift("Dragon", "🐉", 8000, "Awaken an ancient mystical flying flame dragon"),
    Gift("Yacht", "🚢", 12000, "Set sail on premium galactic cosmic star oceans"),
    Gift("Galaxy", "🌌", 20000, "Create a swirling psychedelic interactive solar nebula")
)
