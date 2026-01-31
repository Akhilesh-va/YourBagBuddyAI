package com.example.yourbagbuddy.presentation.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.yourbagbuddy.R
import com.example.yourbagbuddy.presentation.ui.theme.Primary
import com.example.yourbagbuddy.presentation.ui.theme.OnPrimary

@Composable
fun LiquidBottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem(Screen.Home, "Home", Icons.Default.Home),
        BottomNavItem(Screen.Checklist, "Your Checklist", Icons.Default.Menu),
        BottomNavItem(Screen.BestChoices, "Ai choices", Icons.Default.Add, iconRes = R.drawable.ic_ai_icon),
        BottomNavItem(Screen.Profile, "Profile", Icons.Default.Settings)
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    fun isSelected(item: BottomNavItem) =
        item.screen.route == currentRoute || (item.screen == Screen.Checklist && currentRoute?.startsWith("checklist") == true)
    
    val selectedIndex = items.indexOfFirst { isSelected(it) }
        .takeIf { it >= 0 } ?: 0
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                spotColor = Primary.copy(alpha = 0.3f)
            )
            .background(
                Primary,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
    ) {
        // Liquid blob background
        LiquidBlobBackground(
            selectedIndex = selectedIndex,
            itemCount = items.size,
            modifier = Modifier.fillMaxSize()
        )
        
        // Navigation items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                LiquidNavItem(
                    item = item,
                    isSelected = index == selectedIndex,
                    onClick = {
                        navController.navigate(item.screen.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LiquidBlobBackground(
    selectedIndex: Int,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    // Track the width of the container
    var containerWidth by remember { mutableStateOf(0f) }
    
    val itemWidth = if (containerWidth > 0) containerWidth / itemCount else 0f
    val targetCenterX = itemWidth * (selectedIndex + 0.5f)
    
    val animatedCenterX by animateFloatAsState(
        targetValue = targetCenterX,
        animationSpec = tween(durationMillis = 500),
        label = "blob_animation"
    )
    
    Box(
        modifier = modifier.onSizeChanged { size ->
            containerWidth = size.width.toFloat()
        }
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val blobRadius = with(density) { 45.dp.toPx() }
            val blobHeight = with(density) { 75.dp.toPx() }
            val x = if (containerWidth > 0) animatedCenterX else size.width / itemCount * (selectedIndex + 0.5f)
            val topY = size.height - blobHeight
            
            val path = Path().apply {
                // Start from bottom left
                moveTo(0f, size.height)
                lineTo(0f, topY)
                
                // Left wave - more fluid
                cubicTo(
                    x1 = x - blobRadius * 1.5f,
                    y1 = topY,
                    x2 = x - blobRadius * 0.7f,
                    y2 = topY + blobHeight * 0.15f,
                    x3 = x - blobRadius * 0.25f,
                    y3 = topY + blobHeight * 0.35f
                )
                
                // Top liquid blob - smooth rounded curve
                cubicTo(
                    x1 = x - blobRadius * 0.08f,
                    y1 = topY + blobHeight * 0.48f,
                    x2 = x + blobRadius * 0.08f,
                    y2 = topY + blobHeight * 0.48f,
                    x3 = x + blobRadius * 0.25f,
                    y3 = topY + blobHeight * 0.35f
                )
                
                // Right wave - more fluid
                cubicTo(
                    x1 = x + blobRadius * 0.7f,
                    y1 = topY + blobHeight * 0.15f,
                    x2 = x + blobRadius * 1.5f,
                    y2 = topY,
                    x3 = size.width,
                    y3 = topY
                )
                
                lineTo(size.width, size.height)
                close()
            }
            
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Primary.copy(alpha = 0.98f),
                        Primary
                    ),
                    startY = topY,
                    endY = size.height
                )
            )
        }
    }
}

@Composable
fun LiquidNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = tween(durationMillis = 260),
        label = "scale_animation"
    )
    
    val iconColor by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.6f,
        animationSpec = tween(durationMillis = 300),
        label = "color_animation"
    )
    
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (isSelected) 48.dp else 40.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        OnPrimary.copy(alpha = 0.2f)
                    } else {
                        Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (item.iconRes != null) {
                Image(
                    painter = painterResource(item.iconRes),
                    contentDescription = item.label,
                    modifier = Modifier.size(if (isSelected) 28.dp else 24.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    modifier = Modifier.size(if (isSelected) 28.dp else 24.dp),
                    tint = if (isSelected) {
                        OnPrimary
                    } else {
                        Color(0xFF666666).copy(alpha = iconColor)
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = item.label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) {
                OnPrimary
            } else {
                Color(0xFF666666).copy(alpha = iconColor)
            }
        )
    }
}
