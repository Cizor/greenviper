package com.wild.smartrack.data

import com.google.firebase.firestore.DocumentReference

data class Controller(
    var name: String? = null,
    var num_tags: Int = 0,
    var tags: List<DocumentReference>? = null,
    val tag_aggregate: Double = 0.0
)
