package maestro.cli.command

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class IOSDeviceSetupTest {

    @Test
    fun `reports a ready device when all checks succeed`() {
        val setup = IOSDeviceSetup(
            operatingSystemName = { "Mac OS X" },
            commandRunner = FakeCommandRunner(),
            connectedDevices = { listOf(connectedDevice()) },
        )

        val result = setup.run(installDependencies = false)

        assertThat(result.isReady).isTrue()
        assertThat(result.nextStep).isEqualTo(
            "Next, build and sign the XCTest runner with: maestro driver-setup --apple-team-id <APPLE_TEAM_ID> --destination 'platform=iOS,id=00008140-00180D623C52801C'"
        )
    }

    @Test
    fun `installs libimobiledevice when iproxy is missing`() {
        val commandRunner = FakeCommandRunner(
            responses = mutableMapOf(
                "iproxy --version" to false,
            ),
            installMakesIproxyAvailable = true,
        )
        val setup = IOSDeviceSetup(
            operatingSystemName = { "Mac OS X" },
            commandRunner = commandRunner,
            connectedDevices = { listOf(connectedDevice()) },
        )

        val result = setup.run(installDependencies = true)

        assertThat(result.isReady).isTrue()
        assertThat(commandRunner.commands).contains("brew install libimobiledevice")
    }

    @Test
    fun `explains how to install iproxy without changing the system by default`() {
        val commandRunner = FakeCommandRunner(
            responses = mutableMapOf(
                "iproxy --version" to false,
            ),
        )
        val setup = IOSDeviceSetup(
            operatingSystemName = { "Mac OS X" },
            commandRunner = commandRunner,
            connectedDevices = { listOf(connectedDevice()) },
        )

        val result = setup.run(installDependencies = false)

        assertThat(result.isReady).isFalse()
        assertThat(result.nextStep).isEqualTo("Run: brew install libimobiledevice")
        assertThat(commandRunner.commands).doesNotContain("brew install libimobiledevice")
    }

    @Test
    fun `reports a failed Homebrew installation`() {
        val setup = IOSDeviceSetup(
            operatingSystemName = { "Mac OS X" },
            commandRunner = FakeCommandRunner(
                responses = mutableMapOf(
                    "iproxy --version" to false,
                    "brew install libimobiledevice" to false,
                ),
            ),
            connectedDevices = { listOf(connectedDevice()) },
        )

        val result = setup.run(installDependencies = true)

        assertThat(result.isReady).isFalse()
        assertThat(result.nextStep).contains("could not install")
    }

    @Test
    fun `does not attempt macOS setup on another operating system`() {
        val commandRunner = FakeCommandRunner()
        val setup = IOSDeviceSetup(
            operatingSystemName = { "Linux" },
            commandRunner = commandRunner,
            connectedDevices = { listOf(connectedDevice()) },
        )

        val result = setup.run(installDependencies = true)

        assertThat(result.isReady).isFalse()
        assertThat(result.checks.single().name).isEqualTo("macOS")
        assertThat(commandRunner.commands).isEmpty()
    }

    private class FakeCommandRunner(
        private val responses: MutableMap<String, Boolean> = mutableMapOf(),
        private val installMakesIproxyAvailable: Boolean = false,
    ) : IOSDeviceSetup.CommandRunner {
        val commands = mutableListOf<String>()

        override fun run(vararg command: String, inheritOutput: Boolean): IOSDeviceSetup.CommandResult {
            val invocation = command.joinToString(" ")
            commands += invocation
            if (invocation == "brew install libimobiledevice" && installMakesIproxyAvailable) {
                responses["iproxy --version"] = true
            }
            return IOSDeviceSetup.CommandResult(responses[invocation] ?: true)
        }
    }

    private fun connectedDevice() = IOSDeviceSetup.ConnectedDevice(
        name = "Test iPhone",
        udid = "00008140-00180D623C52801C",
    )
}
