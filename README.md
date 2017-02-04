# circe-config

[![Travis CI Status]][Travis CI]
[![Bintray Latest Version Badge]][Bintray Latest Version]

Combines the power of [circe] and awesomeness of [Typesafe config] to
enable straightforward reading of settings into Scala types.

## Usage

To use this library configure your sbt project with the following lines:

```sbt
resolvers += Resolver.bintrayRepo("fonseca", "maven")
libraryDependencies += "io.github.jonas" %% "circe-config" % "0.1.0"
```

## Documentation

 - [API docs](https://jonas.github.io/circe-config/io/github/jonas/circe/config/index.html)

## Example

```scala
import com.typesafe.config.ConfigFactory
import io.circe.generic.auto._
import io.github.jonas.circe.config.syntax._

case class ServerSettings(host: String, port: Int, ssl: Option[String])
case class HttpSettings(server: ServerSettings, version: Double)
case class AppSettings(http: HttpSettings)

val config = ConfigFactory.parseString("""
  http {
    version = 1.1
    server {
      host = localhost
      port = 8080
    }
  }
""")

val serverSettings = config.as[ServerSettings]("http.server")
// => serverSettings: Either[io.circe.Error,ServerSettings] = Right(ServerSettings(localhost,8080,None))

val httpSettings = config.as[ServerSettings]("http")
// => httpSettings: Either[io.circe.Error,HttpSettings] = Right(HttpSettings(ServerSettings(localhost,8080,None),1.1))

val appSettings = config.as[AppSettings]
// => appSettings: Either[io.circe.Error,AppSettings] = Right(AppSettings(HttpSettings(ServerSettings(localhost,8080,None),1.1)))
```

## Releasing

To release version `x.y.z` run:

    > sbt release -Dproject.version=x.y.z

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
 [Travis CI]: https://travis-ci.org/jonas/circe-config
 [Travis CI Status]: https://travis-ci.org/jonas/circe-config.svg?branch=master
 [Bintray Latest Version Badge]: https://api.bintray.com/packages/fonseca/maven/circe-config/images/download.svg
 [Bintray Latest Version]: https://bintray.com/fonseca/maven/circe-config/_latestVersion
