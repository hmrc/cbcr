import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "cbcr"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()

  val compile = Seq(
    "org.reactivemongo" %% "play2-reactivemongo" % "0.12.7-play25",
    "org.reactivemongo" %% "reactivemongo-bson" % "0.12.7-play25",
    "org.reactivemongo" %% "reactivemongo-akkastream" % "0.12.7",
    "com.github.scullxbones" %% "akka-persistence-mongo-rxmongo" % "1.4.3",
    ws,
    "uk.gov.hmrc" %% "auth-client" % "2.2.0",
    "uk.gov.hmrc" %% "play-auth" % "2.5.0",
    "uk.gov.hmrc" %% "microservice-bootstrap" % "6.9.0",
    "uk.gov.hmrc" %% "domain" % "4.1.0",
    "org.typelevel" %% "cats" % "0.9.0" exclude("org.scalacheck","scalacheck_2.11"),
    "com.typesafe.akka" %% "akka-persistence" % "2.4.14",
    "com.github.kxbmap" %% "configs" % "0.4.4",
    "uk.gov.hmrc" %% "emailaddress" % "2.2.0"
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
    "org.eu.acolyte" % "play-reactive-mongo_2.11" % "1.0.43-j7p" % scope
  )

}
