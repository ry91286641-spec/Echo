package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.*
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Initialize ViewModel with application context
                val context = LocalContext.current
                val viewModel = remember { EchoViewModel(context) }
                
                EchoMainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun EchoMainApp(viewModel: EchoViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val activeRoom by viewModel.activeRoom.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val giftAnim by viewModel.activeGiftAnimation.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe active gift animations and auto-dismiss after 3.5 seconds
    LaunchedEffect(giftAnim) {
        if (giftAnim != null) {
            delay(3500)
            viewModel.clearGiftAnimation()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Base structure with Scaffold
        Scaffold(
            bottomBar = {
                if (activeRoom == null) {
                    EchoBottomNavigation(
                        currentTab = currentTab,
                        onTabSelect = { viewModel.currentTab.value = it }
                    )
                }
            },
            containerColor = MidnightBlack
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Main Switcher based on standard tabs
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "TabTransition"
                ) { tab ->
                    when (tab) {
                        "explore" -> ExploreScreen(viewModel = viewModel)
                        "community" -> CommunityScreen(viewModel = viewModel)
                        "wallet" -> WalletScreen(viewModel = viewModel)
                        "creator" -> CreatorScreen(viewModel = viewModel)
                        "admin" -> AdminScreen(viewModel = viewModel)
                        "developer" -> DeveloperScreen()
                    }
                }
            }
        }

        // --- Immersive Room / Screen overlay ---
        AnimatedVisibility(
            visible = activeRoom != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            activeRoom?.let { room ->
                ImmersiveRoomOverlay(
                    room = room,
                    viewModel = viewModel,
                    onLeave = { viewModel.leaveRoom() }
                )
            }
        }

        // --- Global Overlay for Virtual Gift Animations ---
        AnimatedVisibility(
            visible = giftAnim != null,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(400)),
            modifier = Modifier.fillMaxSize()
        ) {
            giftAnim?.let { type ->
                LiveGiftParticleCascade(giftType = type)
            }
        }
    }
}

// --- Custom Jetpack Particle / Visual Effects for luxury gifts ---
@Composable
fun LiveGiftParticleCascade(giftType: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "GiftBounce")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scaler"
    )

    val translationY by infiniteTransition.animateFloat(
        initialValue = 180f,
        targetValue = -220f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "Flyer"
    )

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GiftSpin"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        // Render stylized vector animation background based on giftType
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f
            val centerY = h / 2f

            when (giftType) {
                "Galaxy" -> {
                    // Swirling solar spiral stellar arm elements
                    for (s in 0..3) {
                        val spiralOffset = (s * Math.PI / 2).toFloat()
                        val pSpiral = androidx.compose.ui.graphics.Path()
                        pSpiral.moveTo(centerX, centerY)
                        
                        // Compute golden spiral path points
                        for (i in 0..100) {
                            val theta = (i * 0.15f) + (spinAngle * Math.PI.toFloat() / 180f) + spiralOffset
                            val r = (i * 2.2f) * scale
                            val x = centerX + r * kotlin.math.cos(theta)
                            val y = centerY + r * sin(theta)
                            if (i == 0) pSpiral.moveTo(x, y) else pSpiral.lineTo(x, y)
                        }
                        drawPath(
                            path = pSpiral,
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFD946EF), Color(0xFF3B82F6), Color.Transparent)
                            ),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
                "Dragon" -> {
                    // Swirling flame rings + golden rotating fire orbs
                    for (o in 0..5) {
                        val angle = (spinAngle * Math.PI.toFloat() / 180f) + (o * Math.PI / 3).toFloat()
                        val r = 130.dp.toPx() * scale
                        val x = centerX + r * kotlin.math.cos(angle)
                        val y = centerY + r * sin(angle)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFEF4444), Color(0xFFFBBF24), Color.Transparent)
                            ),
                            radius = 24.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                    }
                }
                "Castle" -> {
                    // Vertical light beams flashing from ground to top sky
                    for (b in 0..3) {
                        val bx = centerX - 120.dp.toPx() + (b * 80.dp.toPx())
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFBBF24).copy(alpha = 0.4f), Color.Transparent)
                            ),
                            start = androidx.compose.ui.geometry.Offset(bx, h),
                            end = androidx.compose.ui.geometry.Offset(bx, 0f),
                            strokeWidth = 24.dp.toPx()
                        )
                    }
                }
                "Yacht" -> {
                    // Star wave oscillations at ocean
                    val wavePath = androidx.compose.ui.graphics.Path()
                    val waveY = centerY + 100.dp.toPx()
                    wavePath.moveTo(0f, waveY)
                    for (xi in 0..50) {
                        val x = (xi.toFloat() / 50f) * w
                        val amp = 24.dp.toPx() * scale
                        val y = waveY + amp * sin((xi * 0.35f) + (spinAngle * Math.PI.toFloat() / 180f))
                        wavePath.lineTo(x, y)
                    }
                    drawPath(
                        path = wavePath,
                        color = Color(0xFF3B82F6),
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
                "Supercar" -> {
                    // Neon speedy track streaks running diagonally
                    for (t in 0..2) {
                        drawLine(
                            color = Color(0xFFEF4444).copy(0.6f),
                            start = androidx.compose.ui.geometry.Offset(0f, centerY + (t * 30.dp.toPx()) - 50.dp.toPx()),
                            end = androidx.compose.ui.geometry.Offset(w, centerY + (t * 30.dp.toPx()) + 100.dp.toPx()),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }
                "Crown" -> {
                    // Drops diamond sparks
                    for (d in 0..12) {
                        val dx = (centerX - 100.dp.toPx()) + (d * 18.dp.toPx()) % 200.dp.toPx()
                        val dy = (spinAngle * 3f + d * 50f) % h
                        drawCircle(
                            color = Color.White.copy(alpha = 0.5f),
                            radius = 2.5.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(dx, dy)
                        )
                    }
                }
            }
        }

        // Animated Gift Box contents
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = translationY.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(235.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFD946EF).copy(0.35f), Color.Transparent),
                                radius = size.minDimension * 0.75f
                            )
                        )
                        // Spinning highlight stroke
                        rotate(spinAngle) {
                            val strokeRadius = size.minDimension * 0.46f * scale
                            drawCircle(
                                color = Color(0xFFFBBF24).copy(0.75f),
                                radius = strokeRadius,
                                style = Stroke(width = 3.5.dp.toPx())
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (giftType) {
                        "Rose" -> "🌹"
                        "Heart" -> "❤️"
                        "Crown" -> "👑"
                        "Supercar" -> "🏎️"
                        "Castle" -> "🏰"
                        "Dragon" -> "🐉"
                        "Yacht" -> "🚢"
                        "Galaxy" -> "🌌"
                        else -> "🎁"
                    },
                    fontSize = (115.sp * scale),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF130E26).copy(0.85f)),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(2.dp, Color(0xFFFBBF24)),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "💎 PREMIUM GIFT BROADCAST 💎",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFBBF24),
                        letterSpacing = 1.4.sp
                    )
                    Text(
                        text = "VVIP item [$giftType] was broadcast to the lobby!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// --- Navigation Controller ---
@Composable
fun EchoBottomNavigation(currentTab: String, onTabSelect: (String) -> Unit) {
    NavigationBar(
        containerColor = MidnightBlack,
        tonalElevation = 8.dp,
        modifier = Modifier.border(1.dp, BorderStrokes, RoundedCornerShape(16.dp))
    ) {
        NavigationBarItem(
            selected = currentTab == "explore",
            onClick = { onTabSelect("explore") },
            icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
            label = { Text("Explore") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonPink,
                selectedTextColor = NeonPink,
                unselectedIconColor = CoolGrey,
                unselectedTextColor = CoolGrey,
                indicatorColor = NeonPurple.copy(alpha = 0.2f)
            ),
            modifier = Modifier.testTag("nav_explore")
        )
        NavigationBarItem(
            selected = currentTab == "community",
            onClick = { onTabSelect("community") },
            icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Community") },
            label = { Text("Feed") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonPink,
                selectedTextColor = NeonPink,
                unselectedIconColor = CoolGrey,
                unselectedTextColor = CoolGrey,
                indicatorColor = NeonPurple.copy(alpha = 0.2f)
            ),
            modifier = Modifier.testTag("nav_community")
        )
        NavigationBarItem(
            selected = currentTab == "wallet",
            onClick = { onTabSelect("wallet") },
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet") },
            label = { Text("Wallet") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonPink,
                selectedTextColor = NeonPink,
                unselectedIconColor = CoolGrey,
                unselectedTextColor = CoolGrey,
                indicatorColor = NeonPurple.copy(alpha = 0.2f)
            ),
            modifier = Modifier.testTag("nav_wallet")
        )
        NavigationBarItem(
            selected = currentTab == "creator",
            onClick = { onTabSelect("creator") },
            icon = { Icon(Icons.Default.BarChart, contentDescription = "Creator") },
            label = { Text("Creator") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonPink,
                selectedTextColor = NeonPink,
                unselectedIconColor = CoolGrey,
                unselectedTextColor = CoolGrey,
                indicatorColor = NeonPurple.copy(alpha = 0.2f)
            ),
            modifier = Modifier.testTag("nav_creator")
        )
        NavigationBarItem(
            selected = currentTab == "admin",
            onClick = { onTabSelect("admin") },
            icon = { Icon(Icons.Default.SettingsSuggest, contentDescription = "Admin") },
            label = { Text("Admin") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonPink,
                selectedTextColor = NeonPink,
                unselectedIconColor = CoolGrey,
                unselectedTextColor = CoolGrey,
                indicatorColor = NeonPurple.copy(alpha = 0.2f)
            ),
            modifier = Modifier.testTag("nav_admin")
        )
        NavigationBarItem(
            selected = currentTab == "developer",
            onClick = { onTabSelect("developer") },
            icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Docs") },
            label = { Text("Docs") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonPink,
                selectedTextColor = NeonPink,
                unselectedIconColor = CoolGrey,
                unselectedTextColor = CoolGrey,
                indicatorColor = NeonPurple.copy(alpha = 0.2f)
            )
        )
    }
}

// --- 1. Explore Tab: Channels & Streaming Rooms ---
@Composable
fun ExploreScreen(viewModel: EchoViewModel) {
    val rooms by viewModel.roomList.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    var newRoomTitle by remember { mutableStateOf("") }
    var newRoomDesc by remember { mutableStateOf("") }
    var newRoomType by remember { mutableStateOf("PUBLIC") }
    var isPrivatePassword by remember { mutableStateOf(false) }
    var passCodeInput by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // Welcoming Premium Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "EchoLive 🎙️",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Gamer. Vocalist. Real-time connections.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoolGrey
                )
            }
            userProfile?.let { profile ->
                Card(
                    shape = CircleShape,
                    border = BorderStroke(2.dp, NeonPink),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(NeonPurple, ElectricCyan)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.username.take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Sub login controller for custom demos
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkGreySurface),
            border = BorderStroke(1.dp, BorderStrokes),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 5.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SIMULATE MULTI-LOGIN REGISTER",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Button(
                            onClick = { viewModel.registerOrLogin("GOOGLE") },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Google Login")
                        }
                    }
                    item {
                        Button(
                            onClick = { viewModel.registerOrLogin("OTP", customPhone = "+1 (888) 123-4567") },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Mobile OTP")
                        }
                    }
                    item {
                        Button(
                            onClick = { viewModel.registerOrLogin("FACEBOOK") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Facebook")
                        }
                    }
                }
            }
        }

        // Quick profile mini card
        userProfile?.let { profile ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkGreySurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.username,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AccentGold),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "LV.${profile.level}",
                                    fontSize = 11.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "GUEST UID: ${profile.phone}. login: ${profile.loginMode}",
                            fontSize = 11.sp,
                            color = CoolGrey
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Circle, contentDescription = "Coins", tint = AccentGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${profile.coins}", fontWeight = FontWeight.Bold, color = AccentGold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Diamond, contentDescription = "Gems", tint = NeonPink, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${profile.gems}", fontWeight = FontWeight.Bold, color = NeonPink)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Create voice room action row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Live Social Channels",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = NeonPurple,
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .testTag("create_room_fab")
                    .size(width = 130.dp, height = 40.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Room", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Live list
        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            items(rooms) { room ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.joinRoom(room) }
                        .testTag("room_item_${room.id}"),
                    colors = CardDefaults.cardColors(containerColor = DarkGreySurface),
                    border = BorderStroke(1.dp, BorderStrokes),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = NeonPink.copy(0.2f)),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = room.type,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = NeonPink,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (room.isPasswordProtected) {
                                        Icon(Icons.Default.Lock, contentDescription = "Locked", tint = AccentGold, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = room.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            // Active speaking lights indicator
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ElectricCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE",
                                    fontSize = 11.sp,
                                    color = ElectricCyan,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = room.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = CoolGrey,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Host: @${room.activeHostName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonPurple
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Headset, contentDescription = "Listeners", tint = CoolGrey, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "142 people list", fontSize = 11.sp, color = CoolGrey)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Create Custom Room Dialog ---
    if (showCreateDialog) {
        Dialog(onDismissRequest = { showCreateDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkGreySurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(2.dp, NeonPurple)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "LAUNCH VOICE CHANNEL",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    OutlinedTextField(
                        value = newRoomTitle,
                        onValueChange = { newRoomTitle = it },
                        label = { Text("Room Title") },
                        modifier = Modifier.fillMaxWidth().testTag("add_room_title"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                    )

                    OutlinedTextField(
                        value = newRoomDesc,
                        onValueChange = { newRoomDesc = it },
                        label = { Text("Short Description") },
                        modifier = Modifier.fillMaxWidth().testTag("add_room_desc"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                    )

                    // Type Picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("PUBLIC", "MUSIC", "GAMING").forEach { type ->
                            Button(
                                onClick = { newRoomType = type },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (newRoomType == type) NeonPink else NeonPurple.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(type, fontSize = 11.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Protect with Password", color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp)
                        Switch(
                            checked = isPrivatePassword,
                            onCheckedChange = { isPrivatePassword = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonPink)
                        )
                    }

                    if (isPrivatePassword) {
                        OutlinedTextField(
                            value = passCodeInput,
                            onValueChange = { passCodeInput = it },
                            label = { Text("Digit Password Code") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Cancel", color = CoolGrey)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                if (newRoomTitle.isNotBlank()) {
                                    viewModel.createRoom(
                                        title = newRoomTitle,
                                        type = newRoomType,
                                        description = newRoomDesc,
                                        isPasswordProtected = isPrivatePassword,
                                        passVal = passCodeInput
                                    )
                                    newRoomTitle = ""
                                    newRoomDesc = ""
                                    showCreateDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                        ) {
                            Text("Launch Live")
                        }
                    }
                }
            }
        }
    }
}

// --- 2. Community Tab: Posts, Polls, and Events ---
@Composable
fun CommunityScreen(viewModel: EchoViewModel) {
    val posts by viewModel.postsList.collectAsStateWithLifecycle()
    var postContentInput by remember { mutableStateOf("") }
    var newPostType by remember { mutableStateOf("POST") } // POST, POLL, EVENT
    
    // Poll specific parameters
    var pollQuestion by remember { mutableStateOf("") }
    var optA by remember { mutableStateOf("") }
    var optB by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "ECHO SQUARE FEED 📰",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(20.dp)
        )

        // Post Creation Field
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkGreySurface),
            border = BorderStroke(1.dp, BorderStrokes),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = postContentInput,
                    onValueChange = { postContentInput = it },
                    placeholder = { Text("Share anything on Echo board... Use @ai suffix to invite companion opinion!") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("community_post_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("POST", "POLL", "EVENT").forEach { type ->
                        FilterChip(
                            selected = newPostType == type,
                            onClick = { newPostType = type },
                            label = { Text(type, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonPink.copy(0.3f),
                                selectedLabelColor = NeonPink
                            )
                        )
                    }
                }

                if (newPostType == "POLL") {
                    OutlinedTextField(
                        value = pollQuestion,
                        onValueChange = { pollQuestion = it },
                        placeholder = { Text("Poll Question") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = optA,
                            onValueChange = { optA = it },
                            placeholder = { Text("Option A") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                        )
                        OutlinedTextField(
                            value = optB,
                            onValueChange = { optB = it },
                            placeholder = { Text("Option B") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (postContentInput.isNotBlank()) {
                                viewModel.insertCommunityFeed(
                                    text = postContentInput,
                                    postType = newPostType,
                                    q = pollQuestion,
                                    opA = optA,
                                    opB = optB,
                                    evtTime = if (newPostType == "EVENT") "Today, 9 PM UTC" else ""
                                )
                                postContentInput = ""
                                pollQuestion = ""
                                optA = ""
                                optB = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                    ) {
                        Text("Broadcast")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Posts list
        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            items(posts) { post ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkGreySurface),
                    border = BorderStroke(1.dp, BorderStrokes)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "@" + post.senderName,
                                fontWeight = FontWeight.ExtraBold,
                                color = ElectricCyan
                            )
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NeonPurple.copy(0.2f)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = post.postType,
                                    fontSize = 9.sp,
                                    color = NeonPurple,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(text = post.content, fontSize = 14.sp)

                        if (post.postType == "POLL" && post.pollQuestion.isNotBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MidnightBlack),
                                border = BorderStroke(1.dp, BorderStrokes)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(text = "📊 " + post.pollQuestion, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = { viewModel.votePoll(post.id, "A") },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(0.3f))
                                        ) {
                                            Text("${post.pollOptionA} (${post.votesA})", fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = { viewModel.votePoll(post.id, "B") },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink.copy(0.3f))
                                        ) {
                                            Text("${post.pollOptionB} (${post.votesB})", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        if (post.postType == "EVENT" && post.eventTime.isNotBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = AccentGold.copy(0.15f)),
                                border = BorderStroke(1.dp, AccentGold)
                            ) {
                                Text(
                                    text = "📅 EVENT LIVE SCHEDULE: " + post.eventTime,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AccentGold,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Broadcasted: Just now",
                                fontSize = 11.sp,
                                color = CoolGrey
                            )
                            IconButton(onClick = { viewModel.likePost(post.id) }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Favorite, contentDescription = "Like", tint = NeonPink, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "${post.likesCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 3. Wallet (Economy & Virtual Recharge) ---
@Composable
fun WalletScreen(viewModel: EchoViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    var showRechargeConfirm by remember { mutableStateOf(false) }
    var selectedCoinsInput by remember { mutableStateOf(1000) }
    var selectedPriceInput by remember { mutableStateOf(0.99) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ECHO WALLET & COINS 🪙",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Wallet Balance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkGreySurface),
            border = BorderStroke(2.dp, NeonPurple)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "CURRENT ECONOMY BALANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = CoolGrey,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = "Coins", tint = AccentGold, modifier = Modifier.size(36.dp))
                    Text(
                        text = "${userProfile?.coins ?: 0}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = AccentGold
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Diamond, contentDescription = "Gems", tint = NeonPink, modifier = Modifier.size(24.dp))
                    Text(
                        text = "${userProfile?.gems ?: 0} Gems Gained",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonPink
                    )
                }
                Text(
                    text = "Gems are earned when viewers send you luxury virtual gifts! You can withdraw gems as cash, or convert them back to Coins.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoolGrey,
                    textAlign = TextAlign.Center
                )
            }
        }

        Text(
            text = "Instant Recharge Packages",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Recharge bundle list
        val packages = listOf(
            Triple(1000, 0.99, "Starter Pack"),
            Triple(5000, 4.99, "Gamer Bundle"),
            Triple(12000, 9.99, "Streamer Pack"),
            Triple(60000, 49.99, "High Roller Deal")
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(packages) { pack ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkGreySurface),
                    border = BorderStroke(1.dp, BorderStrokes),
                    modifier = Modifier.clickable {
                        selectedCoinsInput = pack.first
                        selectedPriceInput = pack.second
                        showRechargeConfirm = true
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "🪙 ${pack.first} Coins", fontWeight = FontWeight.Bold)
                            Text(text = pack.third, style = MaterialTheme.typography.bodySmall, color = CoolGrey)
                        }
                        Button(
                            onClick = {
                                selectedCoinsInput = pack.first
                                selectedPriceInput = pack.second
                                showRechargeConfirm = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                        ) {
                            Text("$${pack.second}")
                        }
                    }
                }
            }
        }
    }

    // --- Mock Recharge Dialog ---
    if (showRechargeConfirm) {
        Dialog(onDismissRequest = { showRechargeConfirm = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkGreySurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(2.dp, NeonPurple)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "SECURE MOCK CHECKOUT",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "You are recharging $selectedCoinsInput Coins into your EchoLive account for $$selectedPriceInput.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = "4111 2222 3333 4444",
                        onValueChange = {},
                        label = { Text("Mock VISA Card Number") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = "12/28",
                            onValueChange = {},
                            label = { Text("Expiry") },
                            modifier = Modifier.weight(1f),
                            enabled = false
                        )
                        OutlinedTextField(
                            value = "777",
                            onValueChange = {},
                            label = { Text("CVV") },
                            modifier = Modifier.weight(1f),
                            enabled = false
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showRechargeConfirm = false }) {
                            Text("Back", color = CoolGrey)
                        }
                        Button(
                            onClick = {
                                viewModel.rechargeWallet(selectedCoinsInput, selectedPriceInput)
                                showRechargeConfirm = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                        ) {
                            Text("Confirm Pay")
                        }
                    }
                }
            }
        }
    }
}

// --- 4. Creator Tab: Analytics & Live Tracking ---
@Composable
fun CreatorScreen(viewModel: EchoViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "FOR CREATORS: MY STUDIO 📊",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Creator Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkGreySurface),
            border = BorderStroke(1.dp, BorderStrokes)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Creator Level: Class ${userProfile?.creatorLevel ?: 1}",
                    fontWeight = FontWeight.Bold,
                    color = NeonPink
                )
                Divider(color = BorderStrokes)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "TOTAL REVENUE", fontSize = 11.sp, color = CoolGrey)
                        Text(text = "$${userProfile?.creatorRevenue ?: 0.0} USD", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AccentGold)
                    }
                    Column {
                        Text(text = "LIVE VISITORS", fontSize = 11.sp, color = CoolGrey)
                        Text(text = "${userProfile?.visitorCount ?: 100} Fans", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ElectricCyan)
                    }
                }
                Divider(color = BorderStrokes)
                Text(
                    text = "Earnings are generated when followers trigger high-level vector gifts (Yachts, Dragons, Crowns) in your streams. Accumulate 50.00 USD to make instant external withdrawals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoolGrey
                )
            }
        }

        // Analytics List
        Text(
            text = "Active Fan Club List",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        val fanClubList = listOf(
            Pair("AuraGamer", "👑 Fan Club Leader"),
            Pair("VoiceLover_9", "🔥 Active Donator"),
            Pair("NeonHeart", "🌻 Daily Subscriber")
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(fanClubList) { fan ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkGreySurface),
                    border = BorderStroke(1.dp, BorderStrokes)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "@${fan.first}", fontWeight = FontWeight.Bold)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NeonPurple.copy(0.2f)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = fan.second,
                                fontSize = 10.sp,
                                color = NeonPurple,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 5. Admin Tab: Moderator and purger ---
@Composable
fun AdminScreen(viewModel: EchoViewModel) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ADMIN COMMAND DECK 🛠️",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkGreySurface),
            border = BorderStroke(1.dp, BorderStrokes),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "System Action Console",
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )

                Button(
                    onClick = {
                        viewModel.resetProfileMetrics()
                        Toast.makeText(context, "Wallet and Level counters have been reset successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Purge Wallet & Level Coins (Reset Demo)")
                }

                Button(
                    onClick = {
                        Toast.makeText(context, "All flag reporting files have been purged.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear All Flag/Report Lists")
                }

                Button(
                    onClick = {
                        Toast.makeText(context, "User Ban protocol engaged successfully.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ban Flagged Spam Accounts")
                }
            }
        }

        // Diagnostic specs
        Card(
            colors = CardDefaults.cardColors(containerColor = MidnightBlack),
            border = BorderStroke(1.dp, BorderStrokes),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "🖥️ SERVER NODE SPECTRA", fontWeight = FontWeight.Bold, color = CoolGrey)
                Text(text = "- Active Client Connections: 4,112", fontSize = 13.sp)
                Text(text = "- Echo Core Port: WebSockets/WS @8080", fontSize = 13.sp)
                Text(text = "- Node.js Event Loop Delay: 4ms (Healthy)", fontSize = 13.sp)
                Text(text = "- PostGreSQL pool limit: 100 max", fontSize = 13.sp)
            }
        }
    }
}

// --- 6. Docs / Developer Tab ---
@Composable
fun DeveloperScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ECHO CORE SYSTEM SPECIFICATION 💻",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "This screen details the precise engineering stack, Postgres schemas, Node web sockets, and APIs satisfying your exact development prompts.",
            style = MaterialTheme.typography.bodySmall,
            color = CoolGrey
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkGreySurface),
                    border = BorderStroke(1.dp, BorderStrokes)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "📂 Postgres Database Schema", fontWeight = FontWeight.ExtraBold, color = NeonPink)
                        Text(
                            text = """
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(30),
    login_mode VARCHAR(20) DEFAULT 'GUEST',
    current_level INT DEFAULT 1,
    coins INT DEFAULT 1000,
    gems INT DEFAULT 0
);

CREATE TABLE live_rooms (
    room_id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    host_id INT REFERENCES users(id),
    category VARCHAR(20) DEFAULT 'PUBLIC',
    is_locked BOOLEAN DEFAULT FALSE,
    password_hash VARCHAR(255)
);

CREATE TABLE messages (
    msg_id SERIAL PRIMARY KEY,
    room_id INT REFERENCES live_rooms(room_id),
    sender_id INT REFERENCES users(id),
    content TEXT,
    voice_duration INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
                            """.trimIndent(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = ElectricCyan
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkGreySurface),
                    border = BorderStroke(1.dp, BorderStrokes)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "🔌 Node.js WebSocket Code Snippet", fontWeight = FontWeight.ExtraBold, color = NeonPurple)
                        Text(
                            text = """
const WebSocket = require('ws');
const wss = new WebSocket.Server({ port: 8080 });

wss.on('connection', (ws) => {
    console.log('Echo Client connected.');
    
    ws.on('message', (message) => {
        const payload = JSON.parse(message);
        // Handle channel routing, gift triggers, PK battler adjustments
        switch(payload.action) {
            case 'JOIN_ROOM':
                ws.roomId = payload.roomId;
                broadcastToRoom(payload.roomId, { action: 'USER_JOINED', username: payload.username });
                break;
            case 'SEND_GIFT':
                broadcastToRoom(ws.roomId, { action: 'ANNOUNCEMENT', giftName: payload.giftName });
                break;
        }
    });
});
                            """.trimIndent(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = ElectricCyan
                        )
                    }
                }
            }
        }
    }
}

// --- 7. Full-Screen Immersive Voice Chat and PK Live Stream Screen Overlay ---
@Composable
fun ImmersiveRoomOverlay(
    room: RoomEntity,
    viewModel: EchoViewModel,
    onLeave: () -> Unit
) {
    val messages by viewModel.roomMessages.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMicMuted.collectAsStateWithLifecycle()
    val micList by viewModel.micQueue.collectAsStateWithLifecycle()
    val liveScoreboard by viewModel.liveScores.collectAsStateWithLifecycle()
    val viewers by viewModel.viewerCount.collectAsStateWithLifecycle()

    var activeTextMsg by remember { mutableStateOf("") }
    
    // Gift Shop Toggle
    var showGiftSheet by remember { mutableStateOf(false) }

    // Inter-room Mini-game Toggle
    var showGameSheet by remember { mutableStateOf(false) }
    var selectedGameTab by remember { mutableStateOf("LUDO") } // LUDO, QUIZ, DICE

    // AI Companion chat toggled inside active stream
    var showAiAssistantSheet by remember { mutableStateOf(false) }

    // VVIP Badge / levels sheet
    var showVipSheet by remember { mutableStateOf(false) }

    // In-room live floating emojis
    val activeEmojis = remember { mutableStateListOf<FloatingEmojiInstance>() }
    var emojiCount by remember { mutableStateOf(0L) }

    // Entrance animation trigger
    var entranceUser by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        entranceUser = viewModel.userProfile.value?.username ?: "VVIP Star"
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Futuristic premium deep navy & starlit sky background canvas
        PremiumRoomBackground()

        // 2. Ambient glowing particle embers rising
        AmbientFloatingLights()

        Column(modifier = Modifier.fillMaxSize()) {
            // Immersive Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLeave) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Leave", tint = Color.White)
                    }
                    Column {
                        Text(text = room.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.Green)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "🎙️ ROOM #${"100${room.id}"} • ONLINE: $viewers", fontSize = 11.sp, color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = room.type.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Button(
                        onClick = onLeave,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Exit Lobby", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // EVENT FLYING BANNER
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.06f)),
                border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🏆 VVIP STREAM PK TOURNAMENT:", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFBBF24))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Send Dragon or Galaxies to claim top ranks!", fontSize = 10.sp, color = Color.White.copy(0.8f))
                }
            }

            // PK STAGE METERS (if PK room)
            if (room.hasActivePk) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⚡ Blue Team (${liveScoreboard.first})", color = Color(0xFF3B82F6), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFBBF24)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "PK ARENA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(text = "Pink Team (${liveScoreboard.second})", color = Color(0xFFD946EF), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Double-colored PK progress bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(11.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFD946EF))
                    ) {
                        val calculatedProgress = if (liveScoreboard.first + liveScoreboard.second == 0) 0.5f 
                                                 else liveScoreboard.first.toFloat() / (liveScoreboard.first + liveScoreboard.second).toFloat()
                        val firstWeight = calculatedProgress.coerceIn(0.02f, 0.98f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(firstWeight)
                                .background(Color(0xFF3B82F6))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f - firstWeight)
                                .background(Color(0xFFD946EF))
                        )
                    }
                }
            }

            // HOST SEAT (Large prominent position at upper center, with dragon aura border sweep)
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Host Seat Frame Wrapper
                val infiniteTransition = rememberInfiniteTransition(label = "HostEnergy")
                val angleRotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(6000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "AuraRotation"
                )
                val auraScale by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "AuraScale"
                )

                Box(
                    modifier = Modifier.size(105.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Animated Dragon Aura Glow background drawing
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeRadius = size.minDimension * 0.44f * auraScale
                        
                        // Rotated energy fields drawing
                        rotate(angleRotation) {
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    colors = listOf(Color(0xFFFBBF24), Color(0xFF2563EB), Color(0xFFD946EF), Color(0xFFFBBF24))
                                ),
                                radius = strokeRadius,
                                style = Stroke(width = 4.dp.toPx())
                            )
                        }

                        // Outer gold glowing halo
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFBBF24).copy(0.18f), Color.Transparent),
                                radius = size.minDimension * 0.5f
                            )
                        )
                    }

                    // Host Profile Image frame
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF21005D), Color(0xFF6750A4))
                                )
                            )
                            .border(2.5.dp, Color(0xFFFBBF24), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = room.activeHostName.firstOrNull()?.toString()?.uppercase() ?: "H",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    // VIP Crown floating on top right of the host seat
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = (-2).dp)
                            .background(Color(0xFFFBBF24), CircleShape)
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👑", fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                // Host name, Crown title & stats indicators
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBBF24)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "👑 VVIP HOST",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = room.activeHostName,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // 10 Seats Glassmorphism Matrix (No.1 to No.10 arranged in a neat 2x5 grid)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(0.04f))
                    .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(24.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "🎤 GLASS BUBBLE SEATS (No.1 - No.10)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White.copy(0.6f),
                    letterSpacing = 1.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // Row 1 (Seats 1-5) & Row 2 (Seats 6-10)
                for (rowIndex in 0..1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (colIndex in 0..4) {
                            val seatIndex = rowIndex * 5 + colIndex
                            val isOccupied = seatIndex < micList.size
                            val seatNumStr = "No.${seatIndex + 1}"
                            
                            val infiniteTransition = rememberInfiniteTransition(label = "VoicePulse")
                            val pulseBorderScale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.15f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = EaseInOutSine),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "borderPulse"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        if (isOccupied) {
                                            Toast.makeText(context, "Seat occupied by ${micList[seatIndex]}!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.joinMicQueue()
                                            Toast.makeText(context, "You claimed Seat $seatNumStr! 🎙️", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isOccupied) Brush.radialGradient(
                                                colors = listOf(Color(0xFF6750A4).copy(0.2f), Color(0xFF21005D).copy(0.6f))
                                            )
                                            else Brush.radialGradient(
                                                colors = listOf(Color.White.copy(0.06f), Color.White.copy(0.02f))
                                            )
                                        )
                                        .border(
                                            width = if (isOccupied && !isMuted) 2.dp * pulseBorderScale else 1.dp,
                                            brush = if (isOccupied) {
                                                if (isMuted) Brush.linearGradient(listOf(Color(0xFF625B71), Color(0xFF625B71)))
                                                else Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFD946EF)))
                                            } else {
                                                Brush.linearGradient(listOf(Color.White.copy(0.12f), Color.White.copy(0.12f)))
                                            },
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isOccupied) {
                                        // Occupied Seat Avatar / Icon
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize(0.85f)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2563EB).copy(0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = micList[seatIndex].firstOrNull()?.toString() ?: "U",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                        }
                                    } else {
                                        // Empty Seat Icon "+" / Micro outline
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Empty Seat",
                                            tint = Color.White.copy(0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isOccupied) {
                                        micList[seatIndex].split(" ").firstOrNull() ?: "User"
                                    } else {
                                        seatNumStr
                                    },
                                    fontSize = 10.sp,
                                    color = if (isOccupied) Color(0xFFFBBF24) else Color.White.copy(0.6f),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(55.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Real-time voice visualizer wavy graph inside active session
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(35.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val waveInfinite = rememberInfiniteTransition(label = "VoiceWavesSmall")
                val animationProgress by waveInfinite.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "WaveProgress"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val baseHeight = height / 2f
                    val pointsCount = 35
                    
                    val path = androidx.compose.ui.graphics.Path()
                    path.moveTo(0f, baseHeight)

                    for (i in 0..pointsCount) {
                        val x = (i.toFloat() / pointsCount.toFloat()) * width
                        val amplitude = if (isMuted) 1.5f else 10f
                        val waveInput = (i * 0.45f) - (animationProgress * 2 * Math.PI)
                        val y = baseHeight + (amplitude * sin(waveInput)).toFloat()
                        path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF3B82F6), Color(0xFFD946EF), Color(0xFFFBBF24))
                        ),
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                }

                Text(
                    text = if (isMuted) "🎤 SPEAKERS MUTED" else "⚡ VOICE STREAM LINK ACTIVE",
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp,
                    color = if (isMuted) Color.White.copy(0.4f) else Color(0xFF3B82F6),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Comments scrolling ticker
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true
                ) {
                    items(messages.reversed()) { msg ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkGreySurface.copy(0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("msg_item_${msg.id}"),
                            border = BorderStroke(1.dp, BorderStrokes.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = AccentGold),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "LV.${msg.senderLevel}",
                                                fontSize = 9.sp,
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = msg.senderName,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = ElectricCyan
                                        )
                                    }
                                    
                                    // Translate & Moderate actions
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(
                                            onClick = { viewModel.translateMessage(msg.id, msg.content, "EN") },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.height(20.dp)
                                        ) {
                                            Text("AI-EN", fontSize = 10.sp, color = NeonPink)
                                        }
                                        TextButton(
                                            onClick = { viewModel.moderateChatMessage(msg) },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.height(20.dp)
                                        ) {
                                            Text("AI-Mod", fontSize = 10.sp, color = AccentGold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = msg.content, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
                                
                                if (msg.translatedText.isNotBlank()) {
                                    Text(
                                        text = msg.translatedText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonPink,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // IMMERSIVE LOBBY ACTION CONTROLS TRAY (Glassmorphism Bottom Tray)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(0.04f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Mic mute/unmute switcher
                IconButton(
                    onClick = { viewModel.toggleMic() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) Color(0xFFEF4444) else Color(0xFF6750A4))
                        .testTag("mute_mic_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = Color.White
                    )
                }

                // 2. Chat messaging line
                OutlinedTextField(
                    value = activeTextMsg,
                    onValueChange = { activeTextMsg = it },
                    placeholder = { Text("Comment... Try @ai for smart companion", fontSize = 11.sp, color = Color.White.copy(0.5f)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(20.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD946EF),
                        unfocusedBorderColor = Color.White.copy(0.2f),
                        unfocusedContainerColor = Color.White.copy(0.05f),
                        focusedContainerColor = Color.White.copy(0.08f)
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (activeTextMsg.isNotBlank()) {
                                    viewModel.sendMessageToRoom(activeTextMsg)
                                    activeTextMsg = ""
                                }
                            },
                            modifier = Modifier.testTag("send_msg_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color(0xFFD946EF))
                        }
                    }
                )

                // 3. Mini Games selector popup
                IconButton(
                    onClick = { showGameSheet = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6))
                ) {
                    Icon(Icons.Default.SportsEsports, contentDescription = "Mini-Games", tint = Color.White)
                }

                // 4. VVIP Status Dashboard button
                IconButton(
                    onClick = { showVipSheet = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFBBF24))
                ) {
                    Icon(Icons.Default.Stars, contentDescription = "VIP Lounge", tint = Color.Black)
                }

                // 5. Intelligent AI Voice Companion trigger
                IconButton(
                    onClick = { showAiAssistantSheet = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEC4899))
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = "AI Companion", tint = Color.White)
                }

                // 6. Floating emoji trigger spawner
                IconButton(
                    onClick = {
                        val emojis = listOf("💖", "🔥", "👍", "🎉", "🚀", "✨")
                        val randomEmoji = emojis.random()
                        activeEmojis.add(
                            FloatingEmojiInstance(
                                id = emojiCount++,
                                text = randomEmoji,
                                initialX = (0.1f + java.util.Random().nextFloat() * 0.8f),
                                scale = 1f,
                                duration = 3000
                            )
                        )
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.12f))
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = "Send Emojis", tint = Color(0xFFEF4444))
                }

                // 7. Virtual gift tray launcher
                IconButton(
                    onClick = { showGiftSheet = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                        .testTag("gift_tray_button")
                ) {
                    Icon(Icons.Default.Redeem, contentDescription = "Virtual Gifts", tint = Color.White)
                }
            }
        }

        // 3. Supercar arrival alert slide panel (overlay)
        entranceUser?.let { username ->
            SupercarEntryNotification(username = username) {
                entranceUser = null
            }
        }

        // 4. Floating bubbles emoji container (overlay)
        LiveRoomFloatingEmojisContainer(instances = activeEmojis) { id ->
            activeEmojis.removeAll { it.id == id }
        }
    }

        // --- Slide Up Trays (Dialog overlays mimicking sheets) ---
        
        // Trays 1: Gift Sheet Dialog
        if (showGiftSheet) {
            Dialog(onDismissRequest = { showGiftSheet = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkGreySurface),
                    border = BorderStroke(2.dp, AccentGold),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "🎁 CHOOSE VIRTUAL GIFT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = AccentGold
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(250.dp)
                        ) {
                            items(GiffShopList) { gift ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.sendVirtualGift(gift)
                                            showGiftSheet = false
                                        }
                                        .testTag("gift_${gift.name}")
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = gift.icon, fontSize = 28.sp)
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(text = gift.name, fontWeight = FontWeight.Bold)
                                            Text(text = gift.description, style = MaterialTheme.typography.bodySmall, color = CoolGrey)
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.sendVirtualGift(gift)
                                            showGiftSheet = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                                    ) {
                                        Text("🪙 ${gift.coinCost}")
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = { showGiftSheet = false },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateBackground),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to Chat", color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }
        }

        // Tray 2: Mini-Games Sheet
        if (showGameSheet) {
            Dialog(onDismissRequest = { showGameSheet = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkGreySurface),
                    border = BorderStroke(2.dp, ElectricCyan),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "🎮 PLAYABLE MINI-GAMES",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = ElectricCyan
                        )

                        // Game Tab Selector (LUDO, QUIZ, DICE)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("LUDO", "QUIZ", "DICE").forEach { tab ->
                                Button(
                                    onClick = { selectedGameTab = tab },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedGameTab == tab) NeonPink else NeonPurple.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(tab, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Playable Area based on selected tab
                        when (selectedGameTab) {
                            "LUDO" -> {
                                val tokenPos by viewModel.ludoTokenPosition.collectAsStateWithLifecycle()
                                val diceVal by viewModel.ludoDiceRoll.collectAsStateWithLifecycle()
                                val gameLog by viewModel.ludoGameLog.collectAsStateWithLifecycle()

                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(text = "Ludo Board Position: $tokenPos / 24", fontWeight = FontWeight.Bold)
                                    // Custom vector path representation representing board progress
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(30.dp)
                                            .background(MidnightBlack, RoundedCornerShape(15.dp))
                                            .padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        for (i in 1..24) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(if (i <= tokenPos) ElectricCyan else CoolGrey.copy(0.3f))
                                                    .weight(1f)
                                            )
                                        }
                                    }
                                    Text(text = "Last dice rolled: $diceVal", color = AccentGold, fontWeight = FontWeight.Bold)
                                    Text(text = gameLog, style = MaterialTheme.typography.bodySmall, color = CoolGrey, textAlign = TextAlign.Center)

                                    Button(
                                        onClick = { viewModel.rollLudoDice() },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                                    ) {
                                        Text("Roll Live Ludo Dice 🎲")
                                    }
                                }
                            }
                            "QUIZ" -> {
                                val currentQuizIdx by viewModel.quizQuestionIndex.collectAsStateWithLifecycle()
                                val quizResult by viewModel.quizResultState.collectAsStateWithLifecycle()
                                val selectedOption by viewModel.quizSelectedOption.collectAsStateWithLifecycle()

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(text = "Culture Quiz (Earn Coins/XP)", fontWeight = FontWeight.Bold, color = AccentGold)
                                    Text(text = "Question ${currentQuizIdx + 1}/3:")
                                    Text(text = viewModel.quizQuestions[currentQuizIdx].question, fontWeight = FontWeight.Bold)

                                    viewModel.quizQuestions[currentQuizIdx].options.forEachIndexed { idx, option ->
                                        Button(
                                            onClick = { viewModel.answerQuizQuestion(idx) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (selectedOption == option) NeonPurple else DarkGreySurface
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            border = BorderStroke(1.dp, BorderStrokes),
                                            enabled = selectedOption == null
                                        ) {
                                            Text(option, fontSize = 12.sp)
                                        }
                                    }

                                    if (quizResult != null) {
                                        Text(
                                            text = quizResult!!,
                                            fontWeight = FontWeight.Bold,
                                            color = if (quizResult!!.startsWith("CORRECT")) ElectricCyan else NeonPink,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Button(
                                            onClick = { viewModel.nextQuizQuestion() },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Next Question")
                                        }
                                    }
                                }
                            }
                            "DICE" -> {
                                val rolledDice by viewModel.diceNumber.collectAsStateWithLifecycle()
                                val betOpt by viewModel.diceBetOption.collectAsStateWithLifecycle()
                                val betAmt by viewModel.diceBetAmount.collectAsStateWithLifecycle()
                                val resultMsg by viewModel.diceGameResult.collectAsStateWithLifecycle()

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "DICE BETTING WHEEL", fontWeight = FontWeight.Bold, color = AccentGold)
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = { viewModel.diceBetOption.value = "High" },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (betOpt == "High") NeonPink else DarkGreySurface
                                            )
                                        ) {
                                            Text("HIGH (4-6)")
                                        }
                                        Button(
                                            onClick = { viewModel.diceBetOption.value = "Low" },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (betOpt == "Low") NeonPink else DarkGreySurface
                                            )
                                        ) {
                                            Text("LOW (1-3)")
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Wager Amount:")
                                        listOf(10, 50, 100).forEach { wager ->
                                            Button(
                                                onClick = { viewModel.diceBetAmount.value = wager },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (betAmt == wager) AccentGold else SlateBackground
                                                )
                                            ) {
                                                Text("$wager", fontSize = 11.sp, color = if (betAmt == wager) Color.Black else MaterialTheme.colorScheme.onBackground)
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = { viewModel.playDiceBetting() },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                                    ) {
                                        Text("Roll Dice & Resolve Wager 🎲")
                                    }

                                    if (resultMsg.isNotBlank()) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = SlateBackground),
                                            modifier = Modifier.padding(8.dp)
                                        ) {
                                            Text(
                                                text = resultMsg,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(12.dp),
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { showGameSheet = false },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateBackground),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to Room", color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }
        }

        // Tray 2.5: VIP Status Sheet Dialog
        if (showVipSheet) {
            Dialog(onDismissRequest = { showVipSheet = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkGreySurface),
                    border = BorderStroke(2.dp, AccentGold),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "👑 VVIP STATUS DASHBOARD",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = AccentGold
                        )

                        // Member Level Indicators
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(AccentGold.copy(0.15f))
                                    .border(1.5.dp, AccentGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Lvl 8", fontWeight = FontWeight.Black, fontSize = 14.sp, color = AccentGold)
                            }
                            Column {
                                Text("VVIP Star Status", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("XP Progress: 34,500 / 50,000", fontSize = 11.sp, color = CoolGrey)
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { 34500f / 50000f },
                                    modifier = Modifier.width(180.dp).height(6.dp).clip(CircleShape),
                                    color = AccentGold,
                                    trackColor = Color.White.copy(0.12f),
                                )
                            }
                        }

                        // Badges & Achievements List
                        Text(
                            text = "OFFICIAL EARNED BADGES",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White.copy(0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val badges = listOf(
                                "👑" to "Gold Host",
                                "🏎️" to "Supercar VIP",
                                "🌌" to "Cosmic Sender",
                                "⚡" to "PK Fighter"
                            )
                            badges.forEach { (emoji, label) ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateBackground),
                                    modifier = Modifier.weight(1f).border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(12.dp)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(emoji, fontSize = 18.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
                                    }
                                }
                            }
                        }

                        // Premium Perks Info
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.04f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("✨ LIVE MEMBER PRIVILEGES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("• Premium sparkling nickname in room chat history.", fontSize = 9.sp, color = Color.White.copy(0.8f))
                                Text("• Custom red supercar slide-in landing visual effect.", fontSize = 9.sp, color = Color.White.copy(0.8f))
                                Text("• Exclusive host dragon sweep rotation aura.", fontSize = 9.sp, color = Color.White.copy(0.8f))
                            }
                        }

                        Button(
                            onClick = { showVipSheet = false },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Acknowledge VIP Status", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Tray 3: AI Companion Sheet
        if (showAiAssistantSheet) {
            Dialog(onDismissRequest = { showAiAssistantSheet = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkGreySurface),
                    border = BorderStroke(2.dp, NeonPurple),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    val promptText by viewModel.aiCompanionMessage.collectAsStateWithLifecycle()
                    val aiReply by viewModel.aiCompanionReply.collectAsStateWithLifecycle()
                    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "✨ AI LIVE COMPANION ASSISTANT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = NeonPink
                        )
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateBackground),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            border = BorderStroke(1.dp, BorderStrokes)
                        ) {
                            LazyColumn(modifier = Modifier.padding(12.dp)) {
                                item {
                                    Text(
                                        text = aiReply,
                                        fontSize = 13.sp,
                                        color = if (isAiLoading) CoolGrey else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = promptText,
                            onValueChange = { viewModel.aiCompanionMessage.value = it },
                            placeholder = { Text("Ask Echo AI Companion anything...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showAiAssistantSheet = false }) {
                                Text("Cancel", color = CoolGrey)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.callAiCompanion() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                enabled = !isAiLoading
                            ) {
                                Text(if (isAiLoading) "Invoking LLM..." else "Query")
                            }
                        }
                    }
                }
            }
        }
    }

// ==========================================
// PREMIUM FUTURISTIC VOICE ROOM COMPONENTS
// ==========================================

data class FloatingEmojiInstance(
    val id: Long,
    val text: String,
    val initialX: Float, // horizontal fraction (0f to 0.9f)
    val scale: Float,
    val duration: Int
)

@Composable
fun PremiumRoomBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "StarsPulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StarAlpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Vertical gradient representing cosmic fantasy sky
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF02001A), // Starlit midnight
                    Color(0xFF0F0C1B), // Nebula purple
                    Color(0xFF1B2C4F)  // Reflective horizon reflection
                )
            )
        )

        // 2. Twinkling little starry pixels
        val starPositions = listOf(
            0.12f to 0.10f, 0.45f to 0.08f, 0.85f to 0.15f,
            0.28f to 0.22f, 0.65f to 0.18f, 0.92f to 0.06f,
            0.06f to 0.28f, 0.38f to 0.32f, 0.72f to 0.24f
        )
        starPositions.forEachIndexed { idx, (sx, sy) ->
            val starAlpha = ((idx + 1) * 0.12f * alphaAnim).coerceIn(0.1f, 1f)
            drawCircle(
                color = Color.White.copy(starAlpha),
                radius = if (idx % 3 == 0) 2.5f.dp.toPx() else 1.5f.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(sx * width, sy * height)
            )
        }

        // 3. Mountains silhouette path
        val mountainPath = androidx.compose.ui.graphics.Path()
        val horizY = height * 0.64f
        mountainPath.moveTo(0f, horizY)
        mountainPath.lineTo(width * 0.24f, height * 0.44f) // Left Peak
        mountainPath.lineTo(width * 0.40f, height * 0.54f) // Valley
        mountainPath.lineTo(width * 0.64f, height * 0.38f) // High Center Peak
        mountainPath.lineTo(width * 0.80f, height * 0.50f) // Right trail
        mountainPath.lineTo(width, horizY)
        mountainPath.lineTo(width, height)
        mountainPath.lineTo(0f, height)
        mountainPath.close()

        drawPath(
            path = mountainPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0A0515), Color(0xFF140726))
            )
        )

        // 4. Lake reflection ripples water effect
        for (i in 0..7) {
            val ry = horizY + (i * 22.dp.toPx())
            val rw = width * (0.35f + (0.06f * i))
            val rx = (width - rw) / 2f
            drawRoundRect(
                color = Color(0xFF3B82F6).copy(0.12f),
                topLeft = androidx.compose.ui.geometry.Offset(rx, ry),
                size = androidx.compose.ui.geometry.Size(rw, 2.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
            )
        }
    }
}

@Composable
fun AmbientFloatingLights() {
    val infiniteTransition = rememberInfiniteTransition(label = "FloatingParticles")
    val sparkY1 by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkY1"
    )
    val sparkY2 by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 0.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(13000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkY2"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Gold flame ember
        drawCircle(
            color = Color(0xFFFBBF24).copy(0.22f),
            radius = 5.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(width * 0.25f, sparkY1 * height)
        )
        // Violet bubble ember
        drawCircle(
            color = Color(0xFFD946EF).copy(0.20f),
            radius = 4.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(width * 0.72f, sparkY2 * height)
        )
        // Electric Blue spark
        drawCircle(
            color = Color(0xFF3B82F6).copy(0.18f),
            radius = 7.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(width * 0.48f, ((sparkY1 + 0.35f) % 1f) * height)
        )
    }
}

@Composable
fun SupercarEntryNotification(username: String, onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(username) {
        visible = true
        delay(3800)
        visible = false
        delay(400)
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 110.dp, start = 16.dp, end = 16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xEC03001C)),
            border = BorderStroke(2.dp, Brush.linearGradient(colors = listOf(Color.Red, Color(0xFFFBBF24)))),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Red),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏎️", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "⚡ LUXURY ENTRANCE SEATS CHAT ⚡",
                        style = androidx.compose.ui.text.TextStyle(
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "VIP @$username cruised in wearing a Golden Supercar!",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LiveRoomFloatingEmojisContainer(
    instances: List<FloatingEmojiInstance>,
    onFinished: (Long) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        instances.forEach { emoji ->
            key(emoji.id) {
                FloatingEmojiItem(emojiInstance = emoji, onFinished = { onFinished(emoji.id) })
            }
        }
    }
}

@Composable
fun FloatingEmojiItem(
    emojiInstance: FloatingEmojiInstance,
    onFinished: () -> Unit
) {
    var activeY by remember { mutableStateOf(0.80f) }
    var activeAlpha by remember { mutableStateOf(1.0f) }

    LaunchedEffect(Unit) {
        val stepCount = 50
        val stepTime = emojiInstance.duration / stepCount
        for (i in 0..stepCount) {
            delay(stepTime.toLong())
            activeY = 0.80f - (0.65f * (i.toFloat() / stepCount.toFloat()))
            activeAlpha = 1.0f - (i.toFloat() / stepCount.toFloat())
        }
        onFinished()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val pxX = (emojiInstance.initialX * maxWidth.value).dp
        val pxY = (activeY * maxHeight.value).dp

        Box(
            modifier = Modifier
                .offset(x = pxX, y = pxY)
                .alpha(activeAlpha)
        ) {
            Text(emojiInstance.text, fontSize = 26.sp)
        }
    }
}
