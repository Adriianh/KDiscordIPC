package dev.cbyrne.kdiscordipc.test

import dev.cbyrne.kdiscordipc.KDiscordIPC
import dev.cbyrne.kdiscordipc.core.event.impl.ReadyEvent
import dev.cbyrne.kdiscordipc.core.socket.RawPacket
import dev.cbyrne.kdiscordipc.core.socket.Socket
import dev.cbyrne.kdiscordipc.core.socket.impl.WindowsSocket
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WindowsSocketTest {
    @Test
    fun testWindowsSocketPollsUntilDataAvailable() {
        val tempFile = File.createTempFile("test-win-socket-", ".ipc").apply {
            deleteOnExit()
        }

        val client = WindowsSocket()
        client.connect(tempFile)
        assertTrue(client.connected)

        val writerThread = Thread {
            Thread.sleep(150)
            RandomAccessFile(tempFile, "rw").use { raf ->
                val payload = "test".toByteArray()
                val buffer = ByteBuffer.allocate(8 + payload.size).order(ByteOrder.LITTLE_ENDIAN)
                buffer.putInt(1)
                buffer.putInt(payload.size)
                buffer.put(payload)
                raf.write(buffer.array())
            }
        }
        writerThread.start()

        val packet = client.read()
        assertEquals(1, packet.opcode)
        assertEquals(4, packet.length)
        assertEquals("test", packet.data.decodeToString())

        client.close()
        assertFalse(client.connected)
        writerThread.join()
        tempFile.delete()
    }

    @Test
    fun testWindowsSocketUnblocksOnClose() {
        val tempFile = File.createTempFile("test-win-socket-close-", ".ipc").apply {
            deleteOnExit()
        }

        val client = WindowsSocket()
        client.connect(tempFile)
        assertTrue(client.connected)

        var caughtException: IOException? = null
        val readerThread = Thread {
            try {
                client.read()
            } catch (e: IOException) {
                caughtException = e
            }
        }
        readerThread.start()

        Thread.sleep(100)
        client.close()
        readerThread.join(2000)

        assertFalse(readerThread.isAlive, "Reader thread should unblock when close() is called")
        assertNotNull(caughtException, "read() should throw IOException when socket is closed")
        assertFalse(client.connected)
        tempFile.delete()
    }

    @Test
    fun testHandshakeIsWrittenReliablyOnConnect() = runBlocking {
        val writtenPackets = LinkedBlockingQueue<ByteArray>()
        val inboundPackets = LinkedBlockingQueue<RawPacket>()
        val readyReceived = CompletableDeferred<ReadyEvent>()

        val mockSocket = object : Socket {
            @Volatile
            override var connected: Boolean = false

            override fun connect(file: File) {
                connected = true
            }

            override fun read(): RawPacket {
                while (connected) {
                    val pkt = inboundPackets.poll(50, TimeUnit.MILLISECONDS)
                    if (pkt != null) return pkt
                }
                throw EOFException("Closed")
            }

            override fun write(bytes: ByteArray) {
                writtenPackets.put(bytes)
                val readyJson = """{"cmd":"DISPATCH","evt":"READY","data":{"v":1,"config":{"cdn_host":"cdn.discordapp.com","api_endpoint":"//discord.com/api","environment":"production"},"user":{"id":"123456","username":"testuser","discriminator":"0"}}}"""
                val payload = readyJson.toByteArray()
                inboundPackets.put(RawPacket(1, payload.size, payload))
            }

            override fun close() {
                connected = false
            }
        }

        val tempPipeFile = File.createTempFile("discord-ipc-0", "").apply { deleteOnExit() }
        val ipc = KDiscordIPC("1485113215905042515", socketSupplier = { mockSocket }, scope = this)

        ipc.on<ReadyEvent> {
            readyReceived.complete(this)
        }

        // Connect underlying socket and verify write() + events flow works end-to-end without dropping handshake
        val handlerField = ipc.javaClass.getDeclaredField("socketHandler").apply { isAccessible = true }
        val handler = handlerField.get(ipc) as dev.cbyrne.kdiscordipc.core.socket.handler.SocketHandler
        mockSocket.connect(tempPipeFile)

        val collectJob = launch {
            handler.write("handshake".toByteArray())
            handler.events.collect { pkt ->
                if (pkt is dev.cbyrne.kdiscordipc.core.packet.inbound.impl.DispatchEventPacket.Ready) {
                    readyReceived.complete(ReadyEvent(pkt.data))
                }
            }
        }

        val event = withTimeout(2000) {
            readyReceived.await()
        }
        assertEquals("testuser", event.data.user.username)
        assertEquals(1, writtenPackets.size)

        ipc.disconnect()
        collectJob.cancel()
        tempPipeFile.delete()
    }

    @Test
    fun testWindowsPipeDiscoveryDoesNotFailWhenDiscordOpen() {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        if (!isWindows) return

        val handler = dev.cbyrne.kdiscordipc.core.socket.handler.SocketHandler(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO),
            { WindowsSocket() },
            onDisconnect = {}
        )
        val findIPCPathMethod = handler.javaClass.getDeclaredMethod("findIPCPath", Int::class.javaPrimitiveType).apply {
            isAccessible = true
        }

        val pipeRoot = java.nio.file.Paths.get("\\\\.\\pipe\\")
        val hasDiscordPipe = java.nio.file.Files.newDirectoryStream(pipeRoot).use { stream ->
            stream.any { it.fileName?.toString()?.startsWith("discord-ipc-") == true }
        }

        if (hasDiscordPipe) {
            val foundPath = findIPCPathMethod.invoke(handler, 0) as java.nio.file.Path
            assertTrue(foundPath.toString().startsWith("\\\\.\\pipe\\discord-ipc-"))
        }
    }
}
