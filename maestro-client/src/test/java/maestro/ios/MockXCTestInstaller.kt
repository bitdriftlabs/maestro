package maestro.ios

import com.google.common.truth.Truth.assertThat
import xcuitest.XCTestClient
import xcuitest.installer.XCTestInstaller

class MockXCTestInstaller(
    private val simulator: Simulator,
    private val port: Int = 22807,
) : XCTestInstaller {

    private companion object {
        const val UI_TEST_RUNNER_APP_BUNDLE_ID = "io.bitdrift.maestro-driver-iosUITests.xctrunner"
    }

    private var attempts = 0

    override fun start(): XCTestClient {
        attempts++
        for (i in 0..simulator.installationRetryCount) {
            assertThat(simulator.runningApps()).doesNotContain(UI_TEST_RUNNER_APP_BUNDLE_ID)
        }
        simulator.installXCTestDriver()
        return XCTestClient("localhost", port)
    }

    override fun uninstall(): Boolean {
        simulator.uninstallXCTestDriver()
        return true
    }

    override fun isChannelAlive(): Boolean {
        return simulator.isXCTestRunnerAlive()
    }

    override fun close() {
        simulator.uninstallXCTestDriver()
    }

    fun assertInstallationRetries(expectedRetries: Int) {
        assertThat(attempts).isEqualTo(expectedRetries)
    }

    data class Simulator(
        val installationRetryCount: Int = 0,
        val shouldInstall: Boolean = true
    ) {

        private val runningApps = mutableListOf<String>()

        fun runningApps() = runningApps

        fun isXCTestRunnerAlive() = runningApps.contains(UI_TEST_RUNNER_APP_BUNDLE_ID)

        fun uninstallXCTestDriver() = runningApps.clear()

        fun installXCTestDriver() = runningApps.add(UI_TEST_RUNNER_APP_BUNDLE_ID)
    }
}
