import sbt.*

object Dependencies {

  val repos = Seq(
    "Local Maven Repo" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
    "Typesafe Repo" at "https://repo.typesafe.com/typesafe/releases/",
    "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases"
  )

  val scalaTest  = "org.scalatest"  %% "scalatest"  % Version.scalaTest  % Test
  val scalaCheck = "org.scalacheck" %% "scalacheck" % Version.scalaCheck % Test

  val logback        = "ch.qos.logback"         % "logback-classic"      % Version.logback
  val logbackJson    = "ch.qos.logback.contrib" % "logback-json-classic" % Version.logbackContrib
  val logbackJackson = "ch.qos.logback.contrib" % "logback-jackson"      % Version.logbackContrib

  lazy val scalapbRuntime = "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion

  val pekkoTyped                      = "org.apache.pekko"  %% "pekko-actor-typed"              % Version.pekko
  val pekkoActorTestkitTyped          = "org.apache.pekko"  %% "pekko-actor-testkit-typed"      % Version.pekko % Test
  val pekkoClusterShardingTyped       = "org.apache.pekko"  %% "pekko-cluster-sharding-typed"   % Version.pekko
  val pekkoPersistence                = ("org.apache.pekko" %% "pekko-persistence-typed"        % Version.pekko)
  val pekkoPersistenceTestKit         = "org.apache.pekko"  %% "pekko-persistence-testkit"      % Version.pekko % Test
  val pekkoPersistenceJdbc            = "org.apache.pekko"  %% "pekko-persistence-jdbc"         % Version.pekko
  val pekkoPersistenceQuery           = "org.apache.pekko"  %% "pekko-persistence-query"        % Version.pekko
  val pekkoDiscovery                  = "org.apache.pekko"  %% "pekko-discovery"                % Version.pekko
  val pekkoDiscoveryKubernetes        = "org.apache.pekko"  %% "pekko-discovery-kubernetes-api" % Version.pekko
  val pekkoManagement                 = "org.apache.pekko"  %% "pekko-management"               % Version.pekko
  val pekkoManagementClusterBootstrap =
    "org.apache.pekko" %% "pekko-management-cluster-bootstrap" % Version.pekko
  val pekkoManagementHttp = "org.apache.pekko" %% "pekko-management-cluster-http" % Version.pekko
  val pekkoHttp           = "org.apache.pekko" %% "pekko-http"                    % Version.pekkoHttp
  val pekkoHttpSprayJson  = "org.apache.pekko" %% "pekko-http-spray-json"         % Version.pekkoHttp
  val pekkoStream         = "org.apache.pekko" %% "pekko-stream"                  % Version.pekko
  val pekkoJackson        = "org.apache.pekko" %% "pekko-serialization-jackson"   % Version.pekko

  val postgres = "org.postgresql" % "postgresql" % Version.postgres

  val sslConfig = "com.typesafe" %% "ssl-config-core" % "0.6.1"
}
