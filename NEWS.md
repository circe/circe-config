# Release notes

## 0.1.1

 - Add release notes.
 - Publish to Maven Central. ([#2])

 [#2]: https://github.com/jonas/circe-config/issues/2

## 0.1.0

Heavily inspired by the [circe-yaml] library the initial version provides:

 - a circe parser module for Typesafe Config instances.
 - an experimental printer module which in some cases truncates JSON numbers
   ([#1]).
 - a syntax module with implicits for decoding inspired by [Ficus].

 [#1]: https://github.com/jonas/circe-config/issues/1
 [circe-yaml]: https://github.com/circe/circe-yaml
 [ficus]: https://github.com/iheartradio/ficus
