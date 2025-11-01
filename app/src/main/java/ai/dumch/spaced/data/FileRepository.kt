package ai.dumch.spaced.data

import android.content.Context
import java.io.File
import java.io.IOException
import kotlin.io.path.createDirectories

class FileRepository(
    private val context: Context, // application context
) {
    fun read(path: String): String {
        val file = resolveInsideHome(path)
        if (!file.exists() || !file.isFile) {
            throw IOException("Invalid file path: $path")
        }
        return file.readText(Charsets.UTF_8)
    }

    fun list(input: String): String {
        val base = resolveInsideHome(input, mustBeDir = true)
        if (!base.exists() || !base.isDirectory) {
            throw IOException("Invalid directory path: ${base.path}")
        }

        val fixedPath = base.path
        val files = base.walkTopDown()
            .filter { it != base }
            .map { file ->
                val relPath = file.relativeTo(base).path
                if (file.isDirectory) "$fixedPath/$relPath/" else "$fixedPath/$relPath"
            }

        return files.joinToString(",", prefix = "[", postfix = "]")
    }

    private fun resolveInsideHome(input: String, mustBeDir: Boolean = false): File {
        val home = context.filesDir.canonicalFile
        // allow leading "/" in input; treat it as relative to home
        val relative = input.removePrefix("/")
        val target = File(home, relative)

        val canonicalTarget = try {
            // If the path doesn't exist yet (e.g., listing a not-yet-created dir),
            // canonicalFile may fail; fall back to absolute & validate later.
            target.canonicalFile
        } catch (_: IOException) {
            target.absoluteFile
        }

        // Prevent escaping outside home
        val homePath = home.path.trimEnd(File.separatorChar) + File.separator
        val targetPath = (if (canonicalTarget.exists()) canonicalTarget else target).absolutePath
        if (!targetPath.startsWith(homePath)) {
            throw SecurityException("Path escapes home directory.")
        }

        if (mustBeDir && canonicalTarget.exists() && !canonicalTarget.isDirectory) {
            throw IOException("Expected directory at: $input")
        }

        return if (canonicalTarget.exists()) canonicalTarget else target
    }
}
