package com.wild.smartrack.data

import com.google.firebase.firestore.DocumentReference

data class Hub (
    var name: String? = null,
    var num_controllers: Int = 0,
    var controllers: List<DocumentReference>? = null
)
