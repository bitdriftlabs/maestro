package ios.devicectl

import util.CommandLineUtils

class DeviceCtlAppLauncher {
    fun launch(deviceId: String, bundleId: String, launchArguments: List<String>) {
        CommandLineUtils.runCommand(
            buildLaunchCommand(deviceId, bundleId, launchArguments),
        )
    }

    internal fun buildLaunchCommand(
        deviceId: String,
        bundleId: String,
        launchArguments: List<String>,
    ): List<String> {
        return listOf(
                "xcrun",
                "devicectl",
                "device",
                "process",
                "launch",
                "--terminate-existing",
                "--device",
                deviceId,
                bundleId,
                "--",
            ) + launchArguments
    }
}
