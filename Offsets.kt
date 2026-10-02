package com.overlay.codm

object Offsets {
    // these are structural — re-derive per version using IDA/Ghidra on libil2cpp.so
    // base = /proc/pid/maps → look for libil2cpp.so range start

    const val ENTITY_LIST        = 0x8A1F2C0L  // GWorld → ULevel → AActors array
    const val ENTITY_COUNT       = 0x8A1F2C8L
    const val LOCAL_PLAYER       = 0x8A20140L
    const val PLAYER_CONTROLLER  = 0x30L
    const val CAMERA_MANAGER     = 0x350L
    const val POV_LOCATION       = 0x10L
    const val POV_ROTATION       = 0x1CL
    const val POV_FOV            = 0x28L

    // actor → root component → relative location
    const val ROOT_COMPONENT     = 0x138L
    const val RELATIVE_LOCATION  = 0x11CL

    // pawn → health
    const val HEALTH             = 0x3A0L
    const val MAX_HEALTH         = 0x3A4L

    // player state → team
    const val PLAYER_STATE       = 0x230L
    const val TEAM_ID            = 0x3C8L

    // bone array for skeleton ESP
    const val BONE_ARRAY         = 0x280L
    const val BONE_COUNT         = 0x288L

    // derivation note:
    // 1. pull libil2cpp.so from APK or /data/app/
    // 2. load in IDA — find GWorld via string "WorldContext"
    // 3. walk UWorld → PersistentLevel → AActors
    // 4. cross-ref with player health string to confirm pawn struct offset
    // update every major patch — minor patches usually don't shift these
}
