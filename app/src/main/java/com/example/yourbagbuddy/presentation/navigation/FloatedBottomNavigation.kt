package com.example.yourbagbuddy.presentation.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.yourbagbuddy.R

@Composable
fun FloatedBottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem(Screen.Home, "Home", Icons.Default.Home),
        BottomNavItem(Screen.Checklist, "Your Checklist", Icons.Default.Menu),
        BottomNavItem(Screen.BestChoices, "Best Choices", Icons.Default.Add, iconRes = R.drawable.ic_ai_icon),
        BottomNavItem(Screen.Profile, "Profile", Icons.Default.Settings)
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val selectedIndex = items.indexOfFirst { it.screen.route == currentRoute }
        .takeIf { it >= 0 } ?: 0
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Keep some breathing room from screen edges,
            // make the bar sit a bit lower so it doesn't
            // visually collide with content like the "Create a trip" button,
            // and add safe-space above the system gesture bar.
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 6.dp)
    ) {
        // Main navigation bar container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // Slightly more compact height so the nav card
                // consumes less vertical space.
                .height(72.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primary)
        ) {
            // Navigation items
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            // Tighter vertical paddings to match the
                            // reduced overall height, while still
                            // giving enough room for icon + label.
                            top = 6.dp,
                            bottom = 8.dp
                        )
                    ),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    FloatedNavItem(
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
}

@Composable
fun FloatedNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Smoothly animate icon scale and size when selection changes
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "icon_scale"
    )
    val iconSize by animateDpAsState(
        targetValue = if (isSelected) 26.dp else 22.dp,
        animationSpec = tween(durationMillis = 220),
        label = "icon_size"
    )
    Column(
        modifier = Modifier
            // Slightly wider so long labels like "Your Checklist"
            // and "Best Choices" can fit better.
            .width(82.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon container with circular background for active state
        Box(
            modifier = Modifier
                // Slightly larger container when selected
                .size(if (isSelected) 40.dp else 34.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
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
                    modifier = Modifier
                        .size(iconSize)
                        .scale(iconScale),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    // Enlarge icon when selected with a subtle animation
                    modifier = Modifier
                        .size(iconSize)
                        .scale(iconScale),
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                    }
                )
            }
        }
        
        // Keep icon and label visually close for a compact look
        Spacer(modifier = Modifier.height(2.dp))
        
        // Label text – only show for unselected items to
        // keep the selected item visually simpler.
        if (!isSelected) {
            Text(
                text = item.label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}
