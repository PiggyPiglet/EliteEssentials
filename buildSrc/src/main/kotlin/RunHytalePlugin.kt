import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.net.URI
import java.security.MessageDigest

/**
 * Custom Gradle plugin for automated Hytale server testing.
 */
open class RunHytalePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("runHytale", RunHytaleExtension::class.java)

        val runTask: TaskProvider<RunServerTask> = project.tasks.register(
            "runServer", 
            RunServerTask::class.java
        ) {
            jarUrl.set(extension.jarUrl)
            group = "hytale"
            description = "Downloads and runs the Hytale server with your plugin"
        }

        project.tasks.findByName("shadowJar")?.let {
            runTask.configure {
                dependsOn(it)
            }
        }
    }
}

open class RunHytaleExtension {
    var jarUrl: String = "https://example.com/hytale-server.jar"
}

open class RunServerTask : DefaultTask() {

    @Input
    val jarUrl = project.objects.property(String::class.java)

    @TaskAction
    fun run() {
        val runDir = File(project.projectDir, "run").apply { mkdirs() }
        val pluginsDir = File(runDir, "plugins").apply { mkdirs() }
        val jarFile = File(runDir, "server.jar")

        val cacheDir = File(
            project.layout.buildDirectory.asFile.get(), 
            "hytale-cache"
        ).apply { mkdirs() }

        val urlHash = MessageDigest.getInstance("SHA-256")
            .digest(jarUrl.get().toByteArray())
            .joinToString("") { "%02x".format(it) }
        val cachedJar = File(cacheDir, "$urlHash.jar")

        if (!cachedJar.exists()) {
            println("Downloading Hytale server from ${jarUrl.get()}")
            try {
                URI.create(jarUrl.get()).toURL().openStream().use { input ->
                    cachedJar.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                println("Server JAR downloaded and cached")
            } catch (e: Exception) {
                println("ERROR: Failed to download server JAR")
                println("Make sure the jarUrl in build.gradle.kts is correct")
                println("Error: ${e.message}")
                return
            }
        } else {
            println("Using cached server JAR")
        }

        cachedJar.copyTo(jarFile, overwrite = true)

        project.tasks.findByName("shadowJar")?.outputs?.files?.firstOrNull()?.let { shadowJar ->
            val targetFile = File(pluginsDir, shadowJar.name)
            shadowJar.copyTo(targetFile, overwrite = true)
            println("Plugin copied to: ${targetFile.absolutePath}")
        } ?: run {
            println("WARNING: Could not find shadowJar output")
        }

        println("Starting Hytale server...")
        println("Press Ctrl+C to stop the server")

        val debugMode = project.hasProperty("debug")
        val javaArgs = mutableListOf<String>()
        
        if (debugMode) {
            javaArgs.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005")
            println("Debug mode enabled. Connect debugger to port 5005")
        }
        
        javaArgs.addAll(listOf("-jar", jarFile.name))

        val process = ProcessBuilder("java", *javaArgs.toTypedArray())
            .directory(runDir)
            .start()

        project.gradle.buildFinished {
            if (process.isAlive) {
                println("\nStopping server...")
                process.destroy()
            }
        }

        Thread {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { println(it) }
            }
        }.start()

        Thread {
            process.errorStream.bufferedReader().useLines { lines ->
                lines.forEach { System.err.println(it) }
            }
        }.start()

        Thread {
            System.`in`.bufferedReader().useLines { lines ->
                lines.forEach {
                    process.outputStream.write((it + "\n").toByteArray())
                    process.outputStream.flush()
                }
            }
        }.start()

        val exitCode = process.waitFor()
        println("Server exited with code $exitCode")
    }
}
