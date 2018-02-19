# circe-config

[![Travis CI Status]][Travis CI]
[![Latest Version Badge]][Latest Version]

Small library for translating between [HOCON], [Java properties], and JSON
documents and circe's JSON AST.

At a high-level it can be used as a [circe] powered front-end for the [Typesafe
config] library to enable boilerplate free loading of settings into Scala types.
More generally it provides parsers and printers for interoperating with
[Typesafe config]'s JSON AST.

 [HOCON]: https://github.com/lightbend/config/blob/master/HOCON.md
 [Java properties]: https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html

## Usage

To use this library configure your sbt project with the following line:

```sbt
libraryDependencies += "io.circe" %% "circe-config" % "0.4.1"
```

## Documentation

 - [API docs](https://circe.github.io/circe-config/io/circe/config/index.html)

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

    > sbt -Dproject.version=x.y.z release

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
 [Typesafe config]: https://github.com/lightbend/config
 [Travis CI]: https://travis-ci.org/circe/circe-config
 [Travis CI Status]: https://travis-ci.org/circe/circe-config.svg?branch=master
 [Latest Version Badge]: https://img.shields.io/maven-central/v/io.circe/circe-config_2.12.svg
 [Latest Version]: https://maven-badges.herokuapp.com/maven-central/io.circe/circe-config_2.12
