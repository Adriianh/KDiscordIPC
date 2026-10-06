package dev.cbyrne.kdiscordipc.core.socket.impl

import dev.cbyrne.kdiscordipc.core.socket.RawPacket
import dev.cbyrne.kdiscordipc.core.socket.Socket
import dev.cbyrne.kdiscordipc.core.util.reverse
import org.newsclub.net.unix.AFUNIXSocket
import org.newsclub.net.unix.AFUNIXSocketAddress
import java.io.DataInputStream
import java.io.File

class UnixSocket : Socket {
    private val socket = AFUNIXSocket.newInstance()
    private val inputStream by lazy { DataInputStream(socket.inputStream) }

    override val connected: Boolean
        get() = socket.isConnected && !socket.isClosed

    override fun connect(file: File) {
        socket.connect(AFUNIXSocketAddress.of(file))
    }

    override fun read(): RawPacket {
        val opcode = inputStream.readInt().reverse()
        val length = inputStream.readInt().reverse()

        val data = ByteArray(length)
        inputStream.readFully(data)

        return RawPacket(opcode, length, data)
    }

    override fun write(bytes: ByteArray) {
        socket.outputStream.write(bytes)
    }

    override fun close() {
        socket.close()
    }
}
