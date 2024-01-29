import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {
  val bootstrapVersion = "7.23.0"
  val hmrcMongoVersion = "0.74.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % bootstrapVersion,
    "uk.gov.hmrc"       %% "domain"                    % "8.1.0-play-28",
    "org.typelevel"     %% "cats-core"                 % "2.0.0",
    "uk.gov.hmrc"       %% "emailaddress"              % "3.7.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"        % hmrcMongoVersion,
  )

  def test(scope: String = "test"): Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"   % bootstrapVersion % scope,
    "com.typesafe.akka"      %% "akka-testkit"             % "2.6.21"         % scope,
    "org.scalatest"          %% "scalatest"                % "3.0.9"          % scope,
    "com.vladsch.flexmark"   % "flexmark-all"              % "0.35.10"        % scope,
    "org.pegdown"            % "pegdown"                   % "1.6.0"          % scope,
    "org.scalatestplus.play" %% "scalatestplus-play"       % "5.0.0"          % scope,
    "org.scalatestplus"      %% "scalatestplus-scalacheck" % "3.1.0.0-RC2"    % scope,
    "org.mockito"            % "mockito-core"              % "3.11.2"         % scope,
    "org.scalacheck"         %% "scalacheck"               % "1.15.0"         % scope,
    "com.github.tomakehurst" % "wiremock-standalone"       % "2.25.0"         % scope,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28"  % hmrcMongoVersion % scope,
    "org.scalatestplus"      %% "scalatestplus-mockito"    % "1.0.0-M2"       % scope,
  )
}
