package util

import org.apache.logging.log4j.kotlin.logger
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.RevFilter
import util.TimeHelper.isWithinOneYear
import java.io.File
import java.util.*

class GitHelper(private val git: Git) : Iterable<String> {

    constructor(repoUrl: String, outDir: File) : this(cloneGit(repoUrl, outDir))
    constructor(repoDir: File) : this(openLocalGit(repoDir))

    companion object {
        private fun cloneGit(repoUrl: String, outDir: File): Git {
            return Git.cloneRepository()
                .setCloneSubmodules(true)
                .setURI(repoUrl)
                .setDirectory(outDir)
                .call()
        }

        private fun openLocalGit(file: File): Git {
            return Git.open(file)
        }
    }


    private val commitFilter = DateFilter(Date().toInstant().toEpochMilli())

    private val commits = getOneCommitPerMonth(
        git.log()
            .setRevFilter(commitFilter)
            .call()
    ).toMutableList()

    fun close() {
        git.close()
    }

    private fun getOneCommitPerMonth(commits: Iterable<RevCommit>): List<RevCommit> {
        val calendar = Calendar.getInstance()
        val commitMap: MutableMap<Int, RevCommit> = mutableMapOf()
        commits.forEach { commit ->
            val commitTime = Date(commit.commitTime.toLong() * 1000)
            calendar.time = commitTime
            val month = calendar[Calendar.MONTH]
            if (!commitMap.contains(month)) {
                commitMap[month] = commit
            }
        }

        return commitMap.values.toList()
    }

    private class DateFilter(val date: Long) : RevFilter() {
        override fun include(walker: RevWalk?, cmit: RevCommit?): Boolean {
            if (cmit != null) {
                return isWithinOneYear(date, cmit.commitTime.toLong() * 1000) //example commitTime 1702406125
            }
            return false
        }

        override fun clone(): RevFilter {
            return this
        }
    }

    override fun iterator(): Iterator<String> {
        return object : Iterator<String> {
            override fun hasNext(): Boolean {
                return commits.isNotEmpty()
            }

            override fun next(): String {
                if (!hasNext()) {
                    throw NoSuchElementException()
                }

                val commit = commits.removeFirst()
                git.reset()
                    .setMode(ResetCommand.ResetType.HARD)
                    .setRef(commit.name)
                    .call()

                logger.debug { "Next commit $commit.name msg: ${commit.fullMessage}" }
                return commit.name
            }
        }
    }
}
