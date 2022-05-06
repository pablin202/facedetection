package com.pdm.fusionfacedetection

import androidx.annotation.DrawableRes

sealed class HeadPoseResult(
    val isValid: Boolean,
    @DrawableRes val idImage: Int?
) {

    class CorrectPose : HeadPoseResult(
        isValid = true,
        idImage = null
    )

    class ZAngleToLeft : HeadPoseResult(
        isValid = false,
        idImage = R.drawable.ic_move_z_right
    )

    class ZAngleToRight : HeadPoseResult(
        isValid = false,
        idImage = R.drawable.ic_move_z_left
    )

    class YAngleToLeft : HeadPoseResult(
        isValid = false,
        idImage = R.drawable.ic_move_y_right
    )

    class YAngleToRight : HeadPoseResult(
        isValid = false,
        idImage = R.drawable.ic_move_y_left
    )

    class XAngleToUpward : HeadPoseResult(
        isValid = false,
        idImage = R.drawable.ic_move_x_downward
    )

    class XAngleToDownward : HeadPoseResult(
        isValid = false,
        idImage = R.drawable.ic_move_x_upward
    )
}
