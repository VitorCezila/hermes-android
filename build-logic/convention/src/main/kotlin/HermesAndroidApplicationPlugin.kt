import org.gradle.api.Plugin
import org.gradle.api.Project

class HermesAndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
        }
    }
}
