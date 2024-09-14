import BuildSettings.*
import Dependencies.*

val commonDeps = Seq(scalaTest, scalaCheck, logback, logbackJson, logbackJackson)

val pekkoDeps = Seq(
  pekkoTyped,
  pekkoActorTestkitTyped,
  pekkoClusterShardingTyped,
  pekkoPersistence,
  pekkoPersistenceTestKit,
  pekkoPersistenceJdbc,
  pekkoPersistenceQuery,
  pekkoDiscovery,
  pekkoDiscoveryKubernetes,
  pekkoManagement,
  pekkoManagementClusterBootstrap,
  pekkoManagementHttp,
  pekkoHttpSprayJson,
  pekkoHttp,
  pekkoStream,
  pekkoJackson
)

val postgresDeps = Seq(postgres)

val excludeLibraryDependencies = Seq(
  ExclusionRule(
    "ssl-config-core_2.13"
  )
)

lazy val `nft-asset` = (project in file("."))
  .aggregate(`nft-asset-protobuf`, `nft-asset-svc`)

lazy val `nft-asset-protobuf` = (project in file("nft-asset-protobuf"))
  .enablePlugins(PekkoGrpcPlugin)
  .settings(pekkoGrpcCodeGeneratorSettings += "server_power_apis")
  .settings(buildSettings *)
  .settings(libraryDependencies ++= commonDeps ++ Seq(scalapbRuntime))

lazy val `nft-asset-svc` = (project in file("nft-asset-svc"))
  .enablePlugins(JavaServerAppPackaging, PekkoGrpcPlugin, JavaAgent)
  .settings(buildSettings *)
  .settings(
    libraryDependencies ++= commonDeps ++ pekkoDeps ++ postgresDeps ++ Seq(sslConfig),
    excludeDependencies ++= excludeLibraryDependencies
  )
  .dependsOn(`nft-asset-protobuf`)
