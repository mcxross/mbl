package xyz.mcxross.mbl.example

import com.github.michaelbull.result.getOrThrow
import xyz.mcxross.mbl.Mbl
import xyz.mcxross.mbl.editor.edit
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun main() {
    val projectRoot = System.getProperty("project.root") ?: error("System property 'project.root' not set")
    val modulePath: Path = Paths.get(projectRoot, "../../packages/coin_template/build/template/bytecode_modules/template.mv")
    val originalBytes = Files.readAllBytes(modulePath)

    val module = Mbl.deserializeModule(originalBytes).getOrThrow {
        throw Exception("Deserialization Failure")
    }

    val editedModule = module.edit {
        replaceConstantString("Template Coin", "My Awesome Coin")
        replaceConstantString("TMPL", "AWSM")
        replaceConstantString("Template Coin Description", "An awesome new coin.")
    }

    val editedBytes = Mbl.serializeModule(editedModule)

    val editedModulePath = Paths.get(projectRoot, "../../mv/tmp.mv")
    Files.write(editedModulePath, editedBytes)
}