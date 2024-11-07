import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {
  val hmrc = "uk.gov.hmrc"
  val playVersion = "play-30"
  val mongoVersion = "2.3.0"
  var bootstrapVersion = "9.5.0"
  val mockitoScalaVersion = "1.17.37"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc"       %% s"domain-$playVersion"            % "9.0.0",
    "org.typelevel"     %% "cats-core"                       % "2.12.0",
    "uk.gov.hmrc"       %% "emailaddress"                    % "3.8.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion"        % mongoVersion,
    "commons-codec"     % "commons-codec"                    % "1.17.1",
  )

  def test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"          %% s"bootstrap-test-$playVersion"  % bootstrapVersion    % Test,
    "uk.gov.hmrc.mongo"    %% s"hmrc-mongo-test-$playVersion" % mongoVersion        % Test,
    "org.mockito"          %% "mockito-scala"                 % mockitoScalaVersion % Test,
    "org.scalatestplus"    %% "scalacheck-1-17"               % "3.2.18.0"          % Test,
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
