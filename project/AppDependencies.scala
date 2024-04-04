import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {
  val bootstrapVersion = "7.23.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc"              %% "bootstrap-backend-play-28"  % bootstrapVersion,
    "uk.gov.hmrc"              %% "domain"                     % "8.1.0-play-28",
    "org.typelevel"            %% "cats-core"                  % "2.0.0" exclude("org.scalacheck","scalacheck_2.12"),
    "uk.gov.hmrc"              %% "emailaddress"               % "3.7.0",
    "uk.gov.hmrc.mongo"        %% "hmrc-mongo-play-28"         % "0.74.0"
  )

  def test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"   % bootstrapVersion % Test,
    "org.scalatest"           %% "scalatest"                % "3.0.9"          % Test,
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.35.10"        % Test,
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "5.0.0"          % Test,
    "org.scalatestplus"       %% "scalatestplus-scalacheck" % "3.1.0.0-RC2"    % Test,
    "org.mockito"             %  "mockito-core"             % "3.11.2"         % Test,
    "org.scalacheck"          %% "scalacheck"               % "1.15.0"         % Test,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"  % "0.74.0"         % Test,
    "org.scalatestplus"       %% "scalatestplus-mockito"    % "1.0.0-M2"       % Test,
  )


  def apply(): Seq[ModuleID] = compile ++ test
}
