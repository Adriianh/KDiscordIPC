package dev.cbyrne.kdiscordipc.core.socket.impl

import dev.cbyrne.kdiscordipc.core.socket.RawPacket
import dev.cbyrne.kdiscordipc.core.socket.Socket
import dev.cbyrne.kdiscordipc.core.util.reverse
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.SocketException

/**
 * A class representing a Windows [Socket]
 * This uses a RandomAccessFile to expose an [InputStream] and [OutputStream]
 */
class WindowsSocket : Socket {
    private lateinit var randomAccessFile: RandomAccessFile

    @Volatile
    private var _connected = false

    override val connected: Boolean
        get() = _connected

    override fun connect(file: File) {
        randomAccessFile = RandomAccessFile(file, "rw")
        _connected = true
    }

    override fun close() {
        _connected = false
        if (::randomAccessFile.isInitialized) {
            randomAccessFile.close()
        }
    }

    @Suppress("ControlFlowWithEmptyBody")
    override fun read(): RawPacket {
        try {
            while (_connected && randomAccessFile.length() == 0L) {
                Thread.sleep(50L)
            }
        } catch (e: IOException) {
            throw EOFException(e.message)
        }

        if (!_connected) {
            throw SocketException("Socket is closed")
        }

        val opcode = randomAccessFile.readInt().reverse()
        val length = randomAccessFile.readInt().reverse()

        val data = ByteArray(length)
        randomAccessFile.readFully(data)

        return RawPacket(opcode, length, data)
    }

    override fun write(bytes: ByteArray) {
        randomAccessFile.write(bytes)
    }
}