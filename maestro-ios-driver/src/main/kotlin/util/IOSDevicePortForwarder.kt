package util

import java.io.Closeable

class IOSDevicePortForwarder : Closeable {

    private var process: Process? = null

    fun start(port: Int, deviceId: String) {
        if (process?.isAlive == true) {
            return
        }

        // Prefer a USB connection and fall back to legacy Wi-Fi pairing. CoreDevice tunnels are
        // handled directly by LocalXCTestInstaller and do not start this process.
        process = ProcessBuilder("iproxy", "--network", "--local", "--udid", deviceId, "$port:$port")
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
    }

    override fun close() {
        process?.let { process ->
            if (process.isAlive) {
                process.destroy()
            }
        }
        process = null
    }
}
