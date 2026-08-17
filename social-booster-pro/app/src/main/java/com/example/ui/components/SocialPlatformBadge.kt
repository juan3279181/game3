package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SocialPlatform

@Composable
fun getPlatformIcon(platform: SocialPlatform): ImageVector {
    return when (platform) {
        SocialPlatform.ALL -> Icons.Default.Public
        SocialPlatform.YOUTUBE -> Icons.Default.PlayArrow
        SocialPlatform.INSTAGRAM -> Icons.Default.CameraAlt
        SocialPlatform.TIKTOK -> Icons.Default.OndemandVideo
        SocialPlatform.TWITTER_X -> Icons.Default.AlternateEmail
        SocialPlatform.TWITCH -> Icons.Default.LiveTv
        SocialPlatform.FACEBOOK -> Icons.Default.ThumbUp
        SocialPlatform.THREADS -> Icons.Default.Share
    }
}

@Composable
fun SocialPlatformBadge(
    platform: SocialPlatform,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(platform.brandColor.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(platform.brandColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getPlatformIcon(platform),
                contentDescription = platform.displayName,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
        if (showLabel) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = platform.displayName,
                color = platform.brandColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}
