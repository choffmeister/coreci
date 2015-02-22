package de.choffmeister.coreci.integrations

import de.choffmeister.coreci._
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import scala.concurrent.duration._

class GitHubIntegrationSpec extends Specification with NoTimeConversions {
  "GitHubIntegration" should {
    "loads archive tarball" in new TestActorSystem {
      within(60.seconds) {
        val github = new GitHubIntegration()
        val urlTempate = "https://api.github.com/repos/github/gitignore/{archive_format}{/ref}"
        val commitId = "b7cc33a99b02fada900d0e4ba6b7bd38a142f064"
        val future = github.archiveTarball(urlTempate, commitId).flatMap { data =>
          data.runFold(0L)(_ + _.length)
        }

        await(future) must be_>=(0L)
      }
    }
  }
}
