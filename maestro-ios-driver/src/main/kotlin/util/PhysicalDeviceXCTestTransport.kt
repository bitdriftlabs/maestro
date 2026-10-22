package util

import java.io.File

/**
 * The connection Maestro uses to reach the XCTest HTTP server on a physical iOS device.
 *
 * CoreDevice exposes a host-reachable IPv6 tunnel for modern Xcode connections. When it is
 * unavailable, Maestro retains its historical localhost + iproxy transport.
 */
sealed interface PhysicalDeviceXCTestTransport {

    val host: String
    val requiresPortForwarder: Boolean

    /** Applies any transport-specific environment needed by the XCTest runner. */
    fun configureRunFile(xcTestRunFile: File)

    data object IProxy : PhysicalDeviceXCTestTransport {
        override val host = "127.0.0.1"
        override val requiresPortForwarder = true

        override fun configureRunFile(xcTestRunFile: File) = Unit
    }

    data class CoreDeviceTunnel(override val host: String) : PhysicalDeviceXCTestTransport {
        override val requiresPortForwarder = false

        override fun configureRunFile(xcTestRunFile: File) {
            CommandLineUtils.runCommand(
                listOf(
                    "plutil",
                    "-replace",
                    "maestro-driver-iosUITests.EnvironmentVariables.MAESTRO_DRIVER_DIRECT_TUNNEL",
                    "-string",
                    "1",
                    xcTestRunFile.absolutePath,
                ),
            )
        }
    }

    companion object {
        /** Uses Xcode's direct tunnel when the connected device advertises one. */
        fun forDevice(device: DeviceCtlResponse.Device): PhysicalDeviceXCTestTransport =
            device.connectionProperties.tunnelIPAddress
                ?.let(::CoreDeviceTunnel)
                ?: IProxy
    }
}
