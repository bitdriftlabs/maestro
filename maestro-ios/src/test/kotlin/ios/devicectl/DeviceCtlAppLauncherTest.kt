package ios.devicectl

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class DeviceCtlAppLauncherTest {

    @Test
    fun `separates device control flags from app launch arguments`() {
        val command = DeviceCtlAppLauncher().buildLaunchCommand(
            deviceId = "device-id",
            bundleId = "io.bitdrift.example.swiftapp.helloworld",
            launchArguments = listOf("-bitdrift-test-scenario", "physical-crash-smoke"),
        )

        assertThat(command).containsExactly(
            "xcrun",
            "devicectl",
            "device",
            "process",
            "launch",
            "--terminate-existing",
            "--device",
            "device-id",
            "io.bitdrift.example.swiftapp.helloworld",
            "--",
            "-bitdrift-test-scenario",
            "physical-crash-smoke",
        ).inOrder()
    }
}
