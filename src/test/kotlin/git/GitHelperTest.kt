package git

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.io.File

class GitHelperTest {

    @Test
    operator fun iterator() {
        val outFile = File("/tmp/libyeartest")
        val gitHelper = GitHelper(
            repoUrl = "https://github.com/secure-software-engineering/phasar",
            outDir = outFile
            )

        gitHelper.forEach {
            println("Consuming $it")
        }

        gitHelper.close()
        outFile.deleteRecursively()
    }
}