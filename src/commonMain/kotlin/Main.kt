
import com.soywiz.kds.fastCastTo
import com.soywiz.korio.async.async
import com.soywiz.korio.async.use
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.VfsOpenMode
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.stream.writeString
import kotlinx.cinterop.toKString
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.system.exitProcess

public fun main(args: Array<String>) {
    runBlocking {
        val parser = ArgParser("LengthMapper")

        val fileName by parser.option(ArgType.String, "file", "f", "Points to the file used in this operation")
        val removeNumbers by parser.option(ArgType.Boolean, "remove-numbers", "rn", "Remove numbers from the file name").default(false)
        val removeSpaces by parser.option(ArgType.Boolean, "remove-spaces", "rs", "Remove spaces from the file name").default(false)
        val output by parser.option(ArgType.String, "output", "o", "Where to output the new file to.")
        parser.parse(args)

        requireNotNull(fileName) { "You must specify a file using the --file or -f Argument." }
        val file = localVfs(fileName!!, true)
        require(file.exists()) { "This file doesn't exist." }
        require(file.isFile()) { "The selected file must be a text file." }

        val contents = async { getFileContents(file, removeNumbers, removeSpaces) }

        val newFile: Deferred<AsyncStream> = async {
            val outputFile = if (output != null) {
                localVfs(output!!)
            } else localVfs("${file.parent.path}/output.json")

            var openStream: AsyncStream? = null
            if (outputFile.exists()) {
                print("The file ${outputFile.path} already exists, do you want to overwrite it? [y/N]: ")
                while (openStream == null) {
                    val response = readLine()
                    when (response?.lowercase()) {
                        "y", "yes", "t", "true" -> {
                            outputFile.delete()
                            openStream = outputFile.open(VfsOpenMode.CREATE)
                        }
                        "n", "no", "f", "false" -> {
                            exitProcess(0)
                        }
                        else -> {
                            print("Could not parse $response to a boolean, please try again. [y/N]: ")
                        }
                    }
                }
                openStream
            } else return@async outputFile.open(VfsOpenMode.CREATE)
        }
        val (map, outputFile) = awaitAll(contents, newFile)

        val jsonString = Json.encodeToString<MutableMap<Int, MutableSet<String>>>(map.fastCastTo())
        (outputFile as AsyncStream).writeString(jsonString)

        println("Finished!")
    }
}

public suspend fun getFileContents(
    file: VfsFile,
    removeNumbers: Boolean,
    removeSpaces: Boolean
): MutableMap<Int, MutableSet<String>> {
    val map = mutableMapOf<Int, MutableSet<String>>()
    for (num in 1..34) {
        map[num] = mutableSetOf()
    }
    file.openInputStream().use {
        readAll().toKString().splitToSequence('\n').forEach { word ->
            var modified = word
            if (removeNumbers) {
                modified = modified.replace(Regex("[0-9]"), "")
            }
            if (removeSpaces) {
                modified = modified.trim()
            }
            map[modified.length]?.add(modified) ?: error("$modified was supposedly longer than the generate max length of 34???")
        }
    }
    return map
}
