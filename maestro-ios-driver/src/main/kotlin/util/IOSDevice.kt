package util

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

enum class IOSDeviceType {
    REAL,
    SIMULATOR
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class DeviceCtlResponse(
    val result: Result
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Result(
        val devices: List<Device>
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Device(
        val identifier: String,
        val deviceProperties: DeviceProperties?,
        val hardwareProperties: HardwareProperties?,
        val connectionProperties: ConnectionProperties,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ConnectionProperties(
        val tunnelState: String,
        /**
         * Host-reachable IPv6 address assigned by CoreDevice for its connection tunnel.
         *
         * This is present for physical devices whose XCTest runner can be contacted directly,
         * without an `iproxy` port forward.
         */
        val tunnelIPAddress: String? = null,
    ) {
        companion object {
            const val CONNECTED  = "connected"
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DeviceProperties(
        val name: String?,
        val osVersionNumber: String?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class HardwareProperties(
        val udid: String?,
        val reality: String?,
    )
}
