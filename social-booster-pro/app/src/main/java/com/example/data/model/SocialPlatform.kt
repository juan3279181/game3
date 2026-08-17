package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.InstagramGradient1
import com.example.ui.theme.TikTokPink
import com.example.ui.theme.TwitchPurple
import com.example.ui.theme.TwitterBlue
import com.example.ui.theme.YouTubeRed

enum class SocialPlatform(
    val displayName: String,
    val brandColor: Color,
    val iconName: String,
    val defaultUrlPrefix: String,
    val urlPlaceholder: String
) {
    ALL(
        displayName = "All",
        brandColor = Color(0xFF8B5CF6),
        iconName = "all",
        defaultUrlPrefix = "",
        urlPlaceholder = ""
    ),
    YOUTUBE(
        displayName = "YouTube",
        brandColor = YouTubeRed,
        iconName = "youtube",
        defaultUrlPrefix = "https://youtube.com/",
        urlPlaceholder = "https://youtube.com/@yourchannel or video link"
    ),
    INSTAGRAM(
        displayName = "Instagram",
        brandColor = InstagramGradient1,
        iconName = "instagram",
        defaultUrlPrefix = "https://instagram.com/",
        urlPlaceholder = "https://instagram.com/yourusername"
    ),
    TIKTOK(
        displayName = "TikTok",
        brandColor = TikTokPink,
        iconName = "tiktok",
        defaultUrlPrefix = "https://tiktok.com/@",
        urlPlaceholder = "https://tiktok.com/@yourusername"
    ),
    TWITTER_X(
        displayName = "X / Twitter",
        brandColor = TwitterBlue,
        iconName = "twitter",
        defaultUrlPrefix = "https://x.com/",
        urlPlaceholder = "https://x.com/yourhandle"
    ),
    TWITCH(
        displayName = "Twitch",
        brandColor = TwitchPurple,
        iconName = "twitch",
        defaultUrlPrefix = "https://twitch.tv/",
        urlPlaceholder = "https://twitch.tv/yourchannel"
    ),
    FACEBOOK(
        displayName = "Facebook",
        brandColor = Color(0xFF1877F2),
        iconName = "facebook",
        defaultUrlPrefix = "https://facebook.com/",
        urlPlaceholder = "https://facebook.com/yourpage"
    ),
    THREADS(
        displayName = "Threads",
        brandColor = Color(0xFF000000),
        iconName = "threads",
        defaultUrlPrefix = "https://threads.net/@",
        urlPlaceholder = "https://threads.net/@yourusername"
    )
}
