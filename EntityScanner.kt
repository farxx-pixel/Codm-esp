package com.overlay.codm

data class PlayerEntity(
    val address: Long,
    val location: Vec3,
    val teamId: Int,
    val health: Float,
    val maxHealth: Float,
    val bones: List<Vec3>   // head, neck, chest, pelvis, hands, feet
)

class EntityScanner(private val mem: MemoryReader) {

    private val base get() = mem.getBase()

    fun getLocalPlayer(): Long =
        mem.readLong(base + Offsets.LOCAL_PLAYER)

    fun getLocalTeam(): Int {
        val lp = getLocalPlayer()
        val ps = mem.readLong(lp + Offsets.PLAYER_STATE)
        return mem.readInt(ps + Offsets.TEAM_ID)
    }

    fun getCameraLocation(): Vec3 {
        val lp      = getLocalPlayer()
        val pc      = mem.readLong(lp + Offsets.PLAYER_CONTROLLER)
        val cm      = mem.readLong(pc + Offsets.CAMERA_MANAGER)
        return mem.readVec3(cm + Offsets.POV_LOCATION)
    }

    fun getCameraFOV(): Float {
        val lp = getLocalPlayer()
        val pc = mem.readLong(lp + Offsets.PLAYER_CONTROLLER)
        val cm = mem.readLong(pc + Offsets.CAMERA_MANAGER)
        return mem.readFloat(cm + Offsets.POV_FOV)
    }

    fun scanEntities(): List<PlayerEntity> {
        val entityList  = mem.readLong(base + Offsets.ENTITY_LIST)
        val entityCount = mem.readInt(base + Offsets.ENTITY_COUNT)
        val localTeam   = getLocalTeam()
        val results     = mutableListOf<PlayerEntity>()

        val cap = entityCount.coerceAtMost(100)
        for (i in 0 until cap) {
            val actorPtr = mem.readLong(entityList + i * 8L)
            if (actorPtr == 0L) continue

            val rootComp = mem.readLong(actorPtr + Offsets.ROOT_COMPONENT)
            if (rootComp == 0L) continue

            val loc    = mem.readVec3(rootComp + Offsets.RELATIVE_LOCATION)
            val health = mem.readFloat(actorPtr + Offsets.HEALTH)
            val maxHp  = mem.readFloat(actorPtr + Offsets.MAX_HEALTH)

            if (health <= 0f || maxHp <= 0f) continue

            val ps     = mem.readLong(actorPtr + Offsets.PLAYER_STATE)
            val teamId = if (ps != 0L) mem.readInt(ps + Offsets.TEAM_ID) else -1

            if (teamId == localTeam) continue  // skip teammates

            val bones  = readBones(actorPtr)

            results.add(PlayerEntity(actorPtr, loc, teamId, health, maxHp, bones))
        }
        return results
    }

    private fun readBones(actorPtr: Long): List<Vec3> {
        val boneArray = mem.readLong(actorPtr + Offsets.BONE_ARRAY)
        if (boneArray == 0L) return emptyList()
        // bone indices: 0=root, 1=pelvis, 2=spine, 7=head — CODM UE4 skeleton
        val indices = listOf(0, 1, 2, 5, 7, 8, 10, 11, 14, 15)
        return indices.map { i ->
            // each FTransform bone is 48 bytes; translation at offset 0
            mem.readVec3(boneArray + i * 48L)
        }
    }
}
