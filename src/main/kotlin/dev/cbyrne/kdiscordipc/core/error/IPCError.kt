package dev.cbyrne.kdiscordipc.core.error

class IPCError(val code: Int, override val message: String) : Error("IPC command failed ($code): $message")
