package commands.parseSBOM

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import org.spdx.jacksonstore.MultiFormatStore
import org.spdx.library.ModelCopyManager
import org.spdx.library.model.Relationship
import org.spdx.library.model.SpdxDocument
import org.spdx.storage.ISerializableModelStore
import org.spdx.storage.simple.InMemSpdxStore
import java.io.FileInputStream
import kotlin.jvm.optionals.getOrNull


class ParseSbom : CliktCommand() {

    private val inputPath by option(
        help = "Path to the file containing the Paths of" +
                "the files to be analyzed."
    )
        .path(mustExist = true, mustBeReadable = true, canBeFile = true)
        .required()

    private val outputPath by option(
        help = "Path in which all analyzer results are stored"
    )
        .path(mustExist = false, mustBeReadable = false, canBeFile = false)
        .required()

    override fun run() = runBlocking {
        val inputFile = inputPath.toFile()
        val modelStore: ISerializableModelStore =
            MultiFormatStore(InMemSpdxStore(), MultiFormatStore.Format.JSON_PRETTY)
        val copyManager = ModelCopyManager()
        FileInputStream(inputFile).use { stream ->
            val documentUri = modelStore.deSerialize(stream, false)
            val document: SpdxDocument = SpdxDocument(modelStore, documentUri, copyManager, false)

            fun printRelationships(relationships: Collection<Relationship>) {
                relationships.forEach { relationship ->
                    println(relationship)
                    relationship.relatedSpdxElement.getOrNull()?.let { element ->
                        println(element)

                        printRelationships(element.relationships)
                    }
                }
            }
//            val relationships = document.relationships
//            printRelationships(relationships)
            val describes = document.documentDescribes
            describes.forEach { describe ->
                println(describe)
                printRelationships(describe.relationships)
            }
            println(document)
        }

    }
}