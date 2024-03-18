import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import commands.AnalyzeVersions
import commands.GetVersions
import commands.Libyears


class Tool : CliktCommand() {

    override fun run() {
        echo("Starting tool and setting up logging")
    }
}


fun main(args: Array<String>) {
    val tool = Tool()
    tool.subcommands(Libyears(), GetVersions(), AnalyzeVersions()).main(args)
}





