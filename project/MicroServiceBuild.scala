import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "cbcr"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()

  val compile = Seq(
    "org.reactivemongo" %% "play2-reactivemongo" % "0.12.0",
    "org.reactivemongo" %% "reactivemongo-bson" % "0.12.0",
    "org.reactivemongo" %% "reactivemongo-akkastream" % "0.12.0",
    "com.github.scullxbones" %% "akka-persistence-mongo-rxmongo" % "1.4.1",
    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" % "5.15.0",
    "uk.gov.hmrc" %% "play-authorisation" % "4.3.0",
    "uk.gov.hmrc" %% "play-health" % "2.1.0",
    "uk.gov.hmrc" %% "play-url-binders" % "2.1.0",
    "uk.gov.hmrc" %% "play-config" % "4.3.0",
    "uk.gov.hmrc" %% "logback-json-logger" % "3.1.0",
    "uk.gov.hmrc" %% "domain" % "4.1.0",
    "org.typelevel" %% "cats" % "0.9.0" exclude("org.scalacheck","scalacheck_2.11"),
    "com.typesafe.akka" %% "akka-persistence" % "2.4.14",
    "com.github.kxbmap" %% "configs" % "0.4.4"
  )

  def test(scope: String = "test,it") = Seq(
    "com.typesafe.akka" %% "akka-testkit" % "2.4.14" % scope,
    "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.4.17.1" % scope ,
    "uk.gov.hmrc" %% "hmrctest" % "2.3.0" % scope,
    "org.scalatest" %% "scalatest" % "2.2.6" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % scope,
    "org.mockito" % "mockito-core" % "1.9.0" % scope,
    "org.scalacheck" %% "scalacheck" % "1.12.6" % scope,
    "org.eu.acolyte" % "play-reactive-mongo_2.11" % "1.0.43-j7p" % scope,
    "org.specs2" %% "specs2-core" % "3.8.9" % scope
  )

}
