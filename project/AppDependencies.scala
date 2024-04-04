import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {
  val hmrc = "uk.gov.hmrc"
  val playVersion = "play-30"
  val mongoVersion = "1.7.0"
  var bootstrapVersion = "8.4.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc"       %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc"       %% s"domain-$playVersion"            % "9.0.0",
    "org.typelevel"     %% "cats-core"                       % "2.10.0",
    "uk.gov.hmrc"       %% "emailaddress"                    % "3.7.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion"        % mongoVersion,
    "commons-codec"     % "commons-codec"                    % "1.16.1"
  )

  def test = Seq(
    "uk.gov.hmrc"          %% s"bootstrap-test-$playVersion"  % bootstrapVersion % Test,
    "com.vladsch.flexmark" % "flexmark-all"                   % "0.64.8"         % Test,
    "org.scalatestplus"    %% "mockito-3-4"                   % "3.2.10.0"       % Test,
    "org.scalatestplus"    %% "scalacheck-1-17"               % "3.2.17.0"       % Test,
    "uk.gov.hmrc.mongo"    %% s"hmrc-mongo-test-$playVersion" % mongoVersion     % Test,
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
