import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "cbcr"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()

  val compile = Seq(
    "org.reactivemongo" %% "play2-reactivemongo" % "0.17.1-play26",
    "org.reactivemongo" %% "reactivemongo-bson" % "0.17.1",
    ws,
    "uk.gov.hmrc" %% "auth-client" % "2.24.0-play-26",
    "uk.gov.hmrc" %% "bootstrap-play-26" % "0.42.0",
    "uk.gov.hmrc" %% "domain" % "5.6.0-play-26",
    "org.typelevel" %% "cats" % "0.9.0" exclude("org.scalacheck","scalacheck_2.11"),
    "com.github.kxbmap" %% "configs" % "0.4.4",
    "uk.gov.hmrc" %% "emailaddress" % "3.2.0",
    "uk.gov.hmrc" %% "simple-reactivemongo" % "7.20.0-play-26"

  )

  def test(scope: String = "test,it") = Seq(
    "com.typesafe.akka" %% "akka-testkit" % "2.4.14" % scope,
    "org.scalatest" %% "scalatest" % "3.0.8" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % scope,
    "org.mockito" % "mockito-core" % "2.28.2" % scope,
    "org.scalacheck" %% "scalacheck" % "1.14.0" % scope,
    "org.eu.acolyte" % "play-reactive-mongo_2.11" % "1.0.43-j7p" % scope,
    "uk.gov.hmrc" %% "bootstrap-play-26" % "0.42.0" % Test classifier "tests"
  )

}
