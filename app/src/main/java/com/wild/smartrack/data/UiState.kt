package com.wild.smartrack.data

enum class UiState {
    IDLE,
    SUCCESS,
    FAILURE
}

enum class UploadStatus {
    SUCCESS,
    FAILURE,
    NONE // used for initial state or reset
}
