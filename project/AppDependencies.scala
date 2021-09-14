import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {
  val compile = Seq(
    ws,
    "org.reactivemongo" %% "play2-reactivemongo"        % "0.18.8-play27",
    "org.reactivemongo" %% "reactivemongo-bson"         % "0.18.8",
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28"  % "5.8.0",
    "uk.gov.hmrc"       %% "domain"                     % "6.1.0-play-28",
    "org.typelevel"     %% "cats"                       % "0.9.0" exclude("org.scalacheck","scalacheck_2.12"),
    "com.github.kxbmap" %% "configs"                    % "0.6.0",
    "uk.gov.hmrc"       %% "emailaddress"               % "3.5.0",
    "uk.gov.hmrc"       %% "simple-reactivemongo"       % "8.0.0-play-28",
    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.5" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.7.5" % Provided cross CrossVersion.full,
    "org.codehaus.woodstox"    % "woodstox-core-asl"    % "4.4.1",
    "msv"                      % "msv"                  % "20050913",
    "com.sun.xml"              % "relaxngDatatype"      % "1.0",
    "com.sun.msv.datatype.xsd" % "xsdlib"               % "2013.2"
  )

  def test(scope: String = "test,it") = Seq(
    "com.typesafe.akka"       %% "akka-testkit"         % "2.6.14"    % scope,
    "org.scalatest"           %% "scalatest"            % "3.0.9"     % scope,
    "com.vladsch.flexmark"    %  "flexmark-all"         % "0.35.10"   % scope,
    "org.pegdown"             %  "pegdown"              % "1.6.0"     % scope,
    "org.scalatestplus.play"  %% "scalatestplus-play"   % "5.0.0"     % scope,
    "org.mockito"             %  "mockito-core"         % "3.11.2"    % scope,
    "org.scalacheck"          %% "scalacheck"           % "1.15.0"    % scope,
    "org.eu.acolyte"          %% "play-reactive-mongo"  % "1.0.50"    % scope,
    "com.github.tomakehurst"  %  "wiremock-standalone"  % "2.25.0"    % scope
  )


  def apply(): Seq[ModuleID] = compile ++ test()
}
