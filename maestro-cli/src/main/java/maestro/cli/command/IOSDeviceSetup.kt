package maestro.cli.command

import util.DeviceCtlResponse
import util.LocalIOSDevice
import java.util.concurrent.TimeUnit

internal class IOSDeviceSetup(
    private val operatingSystemName: () -> String = { System.getProperty("os.name") },
    private val commandRunner: CommandRunner = ProcessCommandRunner(),
    private val connectedDevices: () -> List<ConnectedDevice> = ::connectedDevices,
) {
    fun run(installDependencies: Boolean): Result {
        if (operatingSystemName() != "Mac OS X") {
            return Result(
                checks = listOf(Check("macOS", false, "Physical iOS devices require macOS.")),
                nextStep = null,
            )
        }

        val devicectlAvailable = commandRunner.run("xcrun", "--find", "devicectl").succeeded
        var iproxyAvailable = commandRunner.run("iproxy", "--version").succeeded
        val homebrewAvailable = commandRunner.run("brew", "--version").succeeded
        var installationFailed = false

        if (!iproxyAvailable && installDependencies && homebrewAvailable) {
            installationFailed = !commandRunner.run("brew", "install", "libimobiledevice", inheritOutput = true).succeeded
            iproxyAvailable = commandRunner.run("iproxy", "--version").succeeded
        }

        val physicalDevices = if (devicectlAvailable) {
            runCatching(connectedDevices).getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val nextStep = when {
            installationFailed -> "Homebrew could not install libimobiledevice. Resolve the error above and run this command again."
            !iproxyAvailable && !homebrewAvailable -> "Install Homebrew, then run: brew install libimobiledevice"
            !iproxyAvailable -> "Run: brew install libimobiledevice"
            !devicectlAvailable -> "Install or select Xcode 26 or newer: xcode-select --switch /Applications/Xcode.app/Contents/Developer"
            physicalDevices.isEmpty() -> "Connect and trust an iPhone with Developer Mode enabled, then run this command again."
            else -> {
                val destinationUdid = physicalDevices.singleOrNull()?.udid ?: "<DEVICE_UDID>"
                "Next, build and sign the XCTest runner with: maestro driver-setup --apple-team-id <APPLE_TEAM_ID> --destination 'platform=iOS,id=$destinationUdid'"
            }
        }

        return Result(
            checks = listOf(
                Check("macOS", true, "Detected macOS."),
                Check("Xcode devicectl", devicectlAvailable, "Required for app lifecycle and device discovery."),
                Check("iproxy", iproxyAvailable, "Required to forward the XCTest runner port."),
                Check("connected iPhone", physicalDevices.isNotEmpty(), connectedDeviceDetail(physicalDevices)),
            ),
            nextStep = nextStep,
        )
    }

    data class Result(
        val checks: List<Check>,
        val nextStep: String?,
    ) {
        val isReady: Boolean
            get() = checks.all(Check::succeeded)
    }

    data class Check(
        val name: String,
        val succeeded: Boolean,
        val detail: String,
    )

    data class ConnectedDevice(
        val name: String,
        val udid: String,
    )

    internal interface CommandRunner {
        fun run(vararg command: String, inheritOutput: Boolean = false): CommandResult
    }

    internal data class CommandResult(val succeeded: Boolean)

    private class ProcessCommandRunner : CommandRunner {
        override fun run(vararg command: String, inheritOutput: Boolean): CommandResult {
            return try {
                val processBuilder = ProcessBuilder(*command)
                if (inheritOutput) {
                    processBuilder.inheritIO()
                } else {
                    processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD)
                }

                val process = processBuilder.start()
                if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    return CommandResult(false)
                }

                CommandResult(process.exitValue() == 0)
            } catch (_: Exception) {
                CommandResult(false)
            }
        }
    }

    private companion object {
        const val COMMAND_TIMEOUT_SECONDS = 30L

        fun connectedDevices(): List<ConnectedDevice> = LocalIOSDevice()
            .listDeviceViaDeviceCtl()
            .filter {
                it.connectionProperties.tunnelState == DeviceCtlResponse.ConnectionProperties.CONNECTED &&
                    it.hardwareProperties?.reality == "physical"
            }
            .mapNotNull { device ->
                val udid = device.hardwareProperties?.udid ?: return@mapNotNull null
                ConnectedDevice(
                    name = device.deviceProperties?.name ?: "iPhone",
                    udid = udid,
                )
            }

        fun connectedDeviceDetail(devices: List<ConnectedDevice>): String = when {
            devices.isEmpty() -> "No connected physical iPhones found."
            else -> "Found ${devices.size}: ${devices.joinToString { "${it.name} (UDID: ${it.udid})" }}"
        }
    }
}
