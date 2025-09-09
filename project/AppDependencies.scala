import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {
  val hmrc = "uk.gov.hmrc"
  val playVersion = "play-30"
  val mongoVersion = "2.7.0"
  var bootstrapVersion = "10.1.0"
  val mockitoScalaVersion = "3.2.17.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc"       %% s"domain-$playVersion"            % "13.0.0",
    "org.typelevel"     %% "cats-core"                       % "2.12.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion"        % mongoVersion,
    "commons-codec"     % "commons-codec"                    % "1.17.1",
  )

  def test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"          %% s"bootstrap-test-$playVersion"  % bootstrapVersion    % Test,
    "uk.gov.hmrc.mongo"    %% s"hmrc-mongo-test-$playVersion" % mongoVersion        % Test,
    "org.scalatestplus"    %% "mockito-4-11"                  % mockitoScalaVersion % Test,
    "org.scalatestplus"    %% "scalacheck-1-17"               % "3.2.18.0"          % Test,
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
