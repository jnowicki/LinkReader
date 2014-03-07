name := "LinkReporter"

version := "1.0"

scalaVersion := "2.10.3"

resolvers += "Big Bee Consultants" at "http://repo.bigbeeconsultants.co.uk/repo"

libraryDependencies ++= {
	val akkaVersion = "2.1.4"
	Seq(
  		"com.typesafe.akka" %% "akka-actor"   % akkaVersion,
  		"com.typesafe.akka" %% "akka-slf4j"   % akkaVersion,
  		"ch.qos.logback" % "logback-classic" % "1.0.13",
		"uk.co.bigbeeconsultants" %% "bee-client" % "0.21.+"		
	)
}

seq(Revolver.settings: _*)
