package dev.cbyrne.kdiscordipc.core.util

val temporaryDirectory: String =
    System.getenv("XDG_RUNTIME_DIR")
        ?: System.getenv("TMPDIR")
        ?: System.getenv("TMP")
        ?: System.getenv("TEMP")
        ?: System.getProperty("java.io.tmpdir")
        ?: "/tmp"
