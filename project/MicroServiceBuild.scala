import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "cbcr"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()

  val compile = Seq(
    "org.reactivemongo" %% "play2-reactivemongo" % "0.16.0-play25",
    "org.reactivemongo" %% "reactivemongo-bson" % "0.16.0-play25",
    "org.reactivemongo" %% "reactivemongo-akkastream" % "0.16.1",
    "com.github.scullxbones" %% "akka-persistence-mongo-rxmongo" % "2.2.2",
    ws,
    "uk.gov.hmrc" %% "auth-client" % "2.22.0-play-25",
    "uk.gov.hmrc" %% "bootstrap-play-25" % "4.13.0",
    "uk.gov.hmrc" %% "domain" % "5.6.0-play-25",
    "org.typelevel" %% "cats" % "0.9.0" exclude("org.scalacheck","scalacheck_2.11"),
    "com.typesafe.akka" %% "akka-persistence" % "2.5.23",
    "com.github.kxbmap" %% "configs" % "0.4.4",
    "uk.gov.hmrc" %% "emailaddress" % "3.2.0",
    "uk.gov.hmrc" %% "simple-reactivemongo" % "7.20.0-play-25"

  )

  def test(scope: String = "test,it") = Seq(
    "com.typesafe.akka" %% "akka-testkit" % "2.5.23" % scope,
    "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.1.1" % scope ,
    "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-25" % scope,
    "org.scalatest" %% "scalatest" % "3.0.8" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % scope,
    "org.mockito" % "mockito-core" % "2.28.2" % scope,
    "org.scalacheck" %% "scalacheck" % "1.14.0" % scope,
    "org.eu.acolyte" % "play-reactive-mongo_2.11" % "1.0.43-j7p" % scope
  )

}
