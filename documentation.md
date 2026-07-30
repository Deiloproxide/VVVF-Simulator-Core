<div align="center">

![icon](icon/icon.svg)
# Documentation
How to use this library.
</div>

## Load YAML config
Simple example: see [`YamlLoader.java: YamlLoader.load`][yamlloader] method.

`LoadContext.exception` contains:
- normal
- empty: InputStream opened from an empty file, nothing parsed.
- io: `IOException`
- lex: `org.yaml.snakeyaml.scanner.ScannerException`
- parse: `org.yaml.snakeyaml.parser.ParserException`
- compose: `org.yaml.snakeyaml.composer.ComposerException`
- dump: `org.yaml.snakeyaml.constructor.DuplicateKeyException`
- init: other SnakeYAML exception

if SnakeYAML exception happened, you can get problem position
from `LoadContext.row` and `LoadContext.col`.
## Load Impulse Response
Simple example: see [`IRLoader.java: IRLoader.load`][irloader] method.

`LoadContext.exception` contains:
- normal
- io: `IOException`
- irerror: Error when decode .ir file.
- waverror: Error when decode .wav file.
## Generate PCM audio
Simple example: see [`VVVFSoundGen.java: VVVFSoundGen.mixTo`][vvvfsoundgen] method.

[yamlloader]: https://github.com/Deiloproxide/Create-VVVF-Simulator/blob/main/src/main/java/loader/YamlLoader.java
[irloader]: https://github.com/Deiloproxide/Create-VVVF-Simulator/blob/main/src/main/java/loader/IRLoader.java
[vvvfsoundgen]: https://github.com/Deiloproxide/Create-VVVF-Simulator/blob/main/src/main/java/genengine/VVVFSoundGen.java