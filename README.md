# circe-config

[![Travis CI Status]][Travis CI]
[![Bintray Latest Version Badge]][Bintray Latest Version]

Combines the power of [circe] and awesomeness of [Typesafe config] to
enable straightforward reading of settings into Scala types.

## Usage

To use this library configure your sbt project with the following lines:

```sbt
libraryDependencies += "io.github.jonas" %% "circe-config" % "0.2.1"
```

## Documentation

 - [API docs](https://circe.github.io/circe-config/io/github/jonas/circe/config/index.html)

## Example

```scala
scala> import com.typesafe.config.{ ConfigFactory, ConfigMemorySize }
scala> import io.circe.generic.auto._
scala> import io.circe.config.syntax._
scala> import scala.concurrent.duration.FiniteDuration

scala> case class ServerSettings(host: String, port: Int, timeout: FiniteDuration, maxUpload: ConfigMemorySize)
scala> case class HttpSettings(server: ServerSettings, version: Option[Double])
scala> case class AppSettings(http: HttpSettings)

scala> val config = ConfigFactory.load()

scala> config.as[ServerSettings]("http.server")
res0: Either[io.circe.Error,ServerSettings] = Right(ServerSettings(localhost,8080,5 seconds,ConfigMemorySize(5242880)))

scala> config.as[HttpSettings]("http")
res1: Either[io.circe.Error,HttpSettings] = Right(HttpSettings(ServerSettings(localhost,8080,5 seconds,ConfigMemorySize(5242880)),Some(1.1)))

scala> config.as[AppSettings]
res2: Either[io.circe.Error,AppSettings] = Right(AppSettings(HttpSettings(ServerSettings(localhost,8080,5 seconds,ConfigMemorySize(5242880)),Some(1.1))))
```

Based on this [application.conf].

 [application.conf]: https://github.com/circe/circe-config/tree/master/src/test/resources/application.conf

## Contributing

Contributions are very welcome. Please see [instructions](CONTRIBUTING.md) on
how to create issues and submit patches.

## Releasing

To release version `x.y.z` run:

    > sbt release -Dproject.version=x.y.z

Then go to https://bintray.com/fonseca/maven/circe-config/x.y.z#central and sync
to Maven central.

## License

circe-config is licensed under the **[Apache License, Version 2.0][apache]** (the
"License"); you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

 [apache]: http://www.apache.org/licenses/LICENSE-2.0
 [circe]: https://github.com/circe/circe
 [Typesafe config]: https://github.com/typesafehub/config
 [Travis CI]: https://travis-ci.org/circe/circe-config
 [Travis CI Status]: https://travis-ci.org/circe/circe-config.svg?branch=master
 [Bintray Latest Version Badge]: https://api.bintray.com/packages/fonseca/maven/circe-config/images/download.svg
 [Bintray Latest Version]: https://bintray.com/fonseca/maven/circe-config/_latestVersion
