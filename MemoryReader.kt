package com.overlay.codm

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MemoryReader(private val pid: Int) {

    private val memFile: RandomAccessFile by lazy {
        RandomAccessFile("/proc/$pid/mem", "r")
    }

    private val baseAddress: Long by lazy {
        resolveBase("libil2cpp.so")
    }

    // read raw bytes at absolute address
    fun readBytes(address: Long, size: Int): ByteArray {
        val buf = ByteArray(size)
        try {
            memFile.seek(address)
            memFile.readFully(buf)
        } catch (e: Exception) {
            return ByteArray(size)
        }
        return buf
    }

    fun readInt(address: Long): Int =
        ByteBuffer.wrap(readBytes(address, 4))
            .order(ByteOrder.LITTLE_ENDIAN).int

    fun readLong(address: Long): Long =
        ByteBuffer.wrap(readBytes(address, 8))
            .order(ByteOrder.LITTLE_ENDIAN).long

    fun readFloat(address: Long): Float =
        ByteBuffer.wrap(readBytes(address, 4))
            .order(ByteOrder.LITTLE_ENDIAN).float

    fun readVec3(address: Long): Vec3 = Vec3(
        readFloat(address),
        readFloat(address + 4),
        readFloat(address + 8)
    )

    // resolve libil2cpp.so base from /proc/pid/maps
    fun resolveBase(libName: String): Long {
        File("/proc/$pid/maps").forEachLine { line ->
            if (line.contains(libName) && line.contains("r-xp")) {
                val range = line.split("-")[0]
                return java.lang.Long.parseLong(range, 16)
            }
        }
        return 0L
    }

    fun getBase(): Long = baseAddress

    fun close() = memFile.close()
}

data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    fun distanceTo(other: Vec3): Float {
        val dx = x - other.x; val dy = y - other.y; val dz = z - other.z
        return Math.sqrt((dx*dx + dy*dy + dz*dz).toDouble()).toFloat()
    }
}
