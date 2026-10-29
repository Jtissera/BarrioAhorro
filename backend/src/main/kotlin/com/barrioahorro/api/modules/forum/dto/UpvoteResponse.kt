package com.barrioahorro.api.modules.forum.dto

data class UpvoteResponse(
    val upvotesCount: Int,
    val upvotedByMe: Boolean,
)