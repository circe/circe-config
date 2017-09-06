# Release notes

## master

 - Configure MiMa. [#4], [#8]

 [#4]: https://github.com/circe/circe-config/issues/4
 [#8]: https://github.com/circe/circe-config/pull/8

## 0.3.0

 - circe-config is now part of the [circe GitHub organization](https://github.com/circe).
 - Move everything under the `io.circe.config` package.

## 0.2.1

 - Update circe to version [0.8.0](https://github.com/circe/circe/releases/tag/v0.8.0).
 - Update other dependencies.

## 0.2.0

 - Fix normalization of JSON used for testing the printer law. ([more info][0.1.0-printer-issue])
 - Improve documentation with examples using [sbt-doctest].
 - Rename `printer.defaultOptions` to `printer.DefaultOptions`.
 - Change `syntax.configDecoder` to operate on the JSON AST instead of strings.

 [sbt-doctest]: https://github.com/tkawachi/sbt-doctest

## 0.1.1

 - Add release notes.
 - Publish to Maven Central.

## 0.1.0

Heavily inspired by the [circe-yaml] library the initial version provides:

 - a circe parser module for Typesafe Config instances.
 - an experimental printer module which in some cases truncates JSON numbers ([more info][0.1.0-printer-issue]).
 - a syntax module with implicits for decoding inspired by [Ficus].

 [0.1.0-printer-issue]: https://github.com/circe/circe-config/blob/0.1.0/src/test/scala/io.github.jonas.circe.config/TypesafeConfigSymmetricSerializationTests.scala#L24
 [circe-yaml]: https://github.com/circe/circe-yaml
 [ficus]: https://github.com/iheartradio/ficus
