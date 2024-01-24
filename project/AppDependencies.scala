import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {
  val bootstrapVersion = "7.23.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc"              %% "bootstrap-backend-play-28"  % bootstrapVersion,
    "uk.gov.hmrc"              %% "domain"                     % "8.1.0-play-28",
    "org.typelevel"            %% "cats-core"                  % "2.0.0" exclude("org.scalacheck","scalacheck_2.12"),
    "com.github.kxbmap"        %% "configs"                    % "0.6.0",
    "uk.gov.hmrc"              %% "emailaddress"               % "3.7.0",
    "uk.gov.hmrc.mongo"        %% "hmrc-mongo-play-28"         % "0.74.0",
    "org.codehaus.woodstox"    %  "woodstox-core-asl"          % "4.4.1",
    "msv"                      %  "msv"                        % "20050913",
    "com.sun.xml"              %  "relaxngDatatype"            % "1.0",
    "com.sun.msv.datatype.xsd" %  "xsdlib"                     % "2013.2"
  )

  def test(scope: String = "test,it") = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"   % bootstrapVersion % scope,
    "com.typesafe.akka"       %% "akka-testkit"             % "2.6.21"         % scope,
    "org.scalatest"           %% "scalatest"                % "3.0.9"          % scope,
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.35.10"        % scope,
    "org.pegdown"             %  "pegdown"                  % "1.6.0"          % scope,
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "5.0.0"          % scope,
    "org.scalatestplus"       %% "scalatestplus-scalacheck" % "3.1.0.0-RC2"    % Test,
    "org.mockito"             %  "mockito-core"             % "3.11.2"         % scope,
    "org.scalacheck"          %% "scalacheck"               % "1.15.0"         % scope,
    "com.github.tomakehurst"  %  "wiremock-standalone"      % "2.25.0"         % scope,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"  % "0.74.0"         % scope,
    "org.scalatestplus"       %% "scalatestplus-mockito"    % "1.0.0-M2"       % Test,
  )


  def apply(): Seq[ModuleID] = compile ++ test()
}
