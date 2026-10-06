package dev.cbyrne.kdiscordipc.test

import dev.cbyrne.kdiscordipc.KDiscordIPC
import dev.cbyrne.kdiscordipc.core.event.impl.DisconnectedEvent
import dev.cbyrne.kdiscordipc.core.socket.Socket
import dev.cbyrne.kdiscordipc.core.socket.impl.UnixSocket
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.newsclub.net.unix.AFUNIXServerSocket
import org.newsclub.net.unix.AFUNIXSocketAddress
import java.io.EOFException
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnixSocketTest {
    @Test
    fun testUnixSocketReadAndEOF() {
        val socketFile = File.createTempFile("test-unix-socket-", ".sock").apply { delete() }
        socketFile.deleteOnExit()

        val server = AFUNIXServerSocket.newInstance()
        server.bind(AFUNIXSocketAddress.of(socketFile))

        val client = UnixSocket()
        val serverThread = Thread {
            val serverConn = server.accept()
            val out = serverConn.outputStream

            // Write one packet: opcode=1, length=4, data="test"
            val buffer = ByteBuffer.allocate(4 + 4 + 4).order(ByteOrder.LITTLE_ENDIAN)
            buffer.putInt(1)
            buffer.putInt(4)
            buffer.put("test".toByteArray())
            out.write(buffer.array())
            out.flush()

            // Wait a tiny bit and close to simulate Discord closing
            Thread.sleep(100)
            serverConn.close()
        }
        serverThread.start()

        client.connect(socketFile)
        assertTrue(client.connected)

        // Read the packet
        val packet = client.read()
        assertEquals(1, packet.opcode)
        assertEquals(4, packet.length)
        assertEquals("test", packet.data.decodeToString())

        // Next read should throw EOFException because server closed the connection
        assertFailsWith<EOFException> {
            client.read()
        }

        client.close()
        assertFalse(client.connected)
        server.close()
        socketFile.delete()
        serverThread.join()
    }

    @Test
    fun testSocketHandlerCleanDisconnectOnEOF() = runBlocking {
        val socketFile = File.createTempFile("test-handler-", ".sock").apply { delete() }
        socketFile.deleteOnExit()

        val server = AFUNIXServerSocket.newInstance()
        server.bind(AFUNIXSocketAddress.of(socketFile))

        var disconnected = false
        val handler = dev.cbyrne.kdiscordipc.core.socket.handler.SocketHandler(
            this,
            { UnixSocket() },
            onDisconnect = { disconnected = true }
        )

        val serverThread = Thread {
            val serverConn = server.accept()
            Thread.sleep(100)
            serverConn.close()
        }
        serverThread.start()

        // Directly connect the underlying socket
        val socketField = handler.javaClass.getDeclaredField("socket").apply { isAccessible = true }
        val socket = socketField.get(handler) as UnixSocket
        socket.connect(socketFile)

        val collectJob = launch {
            handler.events.collect {}
        }

        serverThread.join()
        // Wait for handler to process EOF
        withTimeout(2000) {
            while (!disconnected) {
                kotlinx.coroutines.delay(50)
            }
        }

        assertTrue(disconnected)
        assertFalse(handler.connected)
        collectJob.cancel()
        server.close()
        socketFile.delete()
    }
}
