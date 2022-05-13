package com.pdm.fusionfacedetection

interface FaceResults {
    fun faceVisibility(visible: Boolean)
    fun neutralExpression(isNeutral: Boolean)
    fun leftEyeStatus(isOpen: Boolean)
    fun rightEyeStatus(isOpen: Boolean)
    fun headPose(headPoseResult: HeadPoseResult)
    fun requirementsMet(value: Boolean)
}
