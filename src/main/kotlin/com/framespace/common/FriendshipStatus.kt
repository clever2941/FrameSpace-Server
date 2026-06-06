package com.framespace.common

object FriendshipStatus {
    const val NONE = "NONE"
    const val PENDING_SENT = "PENDING_SENT"
    const val PENDING_RECEIVED = "PENDING_RECEIVED"
    const val FRIENDS = "FRIENDS"
    const val SELF = "SELF"
}

object FriendRequestStatus {
    const val PENDING = "PENDING"
    const val ACCEPTED = "ACCEPTED"
    const val REJECTED = "REJECTED"
}

object MessageType {
    const val TEXT = "TEXT"
    const val MOVIE_PUSH = "MOVIE_PUSH"
}
