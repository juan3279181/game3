package com.example.data.model

enum class CampaignType(
    val title: String,
    val actionVerb: String,
    val iconName: String,
    val minCostPerAction: Int
) {
    FOLLOWERS(
        title = "Followers",
        actionVerb = "Follow Profile",
        iconName = "person_add",
        minCostPerAction = 20
    ),
    SUBSCRIBERS(
        title = "Subscribers",
        actionVerb = "Subscribe to Channel",
        iconName = "subscriptions",
        minCostPerAction = 20
    ),
    LIKES(
        title = "Likes & Reactions",
        actionVerb = "Like Post",
        iconName = "favorite",
        minCostPerAction = 15
    ),
    VIEWS(
        title = "Video Views",
        actionVerb = "Watch Video (30s)",
        iconName = "play_circle",
        minCostPerAction = 10
    ),
    COMMENTS(
        title = "Comments",
        actionVerb = "Leave Comment",
        iconName = "comment",
        minCostPerAction = 25
    )
}
