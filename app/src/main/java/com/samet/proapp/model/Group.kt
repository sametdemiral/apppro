package com.samet.proapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Group(
    val id: Int = 0,
    val name: String
) : Parcelable