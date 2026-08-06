<div align="center">

![icon](icon/icon.svg)
# Documentation
How to use VVVF-Simulator-Core in Java applications.
</div>

## Overview
VVVF-Simulator-Core is a pure Java library that ports the core calculation,
configuration, and audio-generation logic from VVVF-Simulator.

The library is suitable for:
- Gradle or Maven applications that need VVVF waveform calculation.
- Offline tools that convert VVVF-Simulator YAML configs into audio.
- Audio applications that need train motor sound, gear sound, harmonic sound,
  impulse-response reverb, or FFT convolution.
## Content
- [For Users](#for-users)
- [For Developers](#for-developers)
- [Package Map](#package-map)
- [Examples](#examples)
## For Users
### Installation
#### Prebuilt Artifact
You can get this library from [GitHub Release](https://github.com/Deiloproxide/VVVF-Simulator-Core/releases).
#### Build From Source
The repository uses Java 17 to run Gradle.
```bash
./gradlew build
```
The current build writes platform artifacts to `/build/libs` directory.
## For Developers
### Dependencies
#### Common Library
The current Gradle project depends on:
- `com.github.wendykierp:JTransforms:3.2`
- `org.visnow:JLargeArrays:1.7`
- `org.apache.commons:commons-math3:3.6.1`
- `org.yaml:snakeyaml:2.6`

You can find them on the MavenCentral.
#### Core Library
Maven configuration:
- pom
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```
- dependency
```xml
<dependency>
    <groupId>com.github.deiloproxide</groupId>
    <artifactId>VVVF-Simulator-Core</artifactId>
    <version>[version]</version>
</dependency>
```
Gradle configuration:
- Groovy DSL
```groovy
repositories{
    maven{url="https://jitpack.io"}
}
dependencies{
    implementation("com.github.deiloproxide:VVVF-Simulator-Core:[version]")
}
```
- Kotlin DSL
```kotlin
repositories{
    maven(url=uri("https://jitpack.io"))
}
dependencies{
    implementation("com.github.deiloproxide:VVVF-Simulator-Core:[version]")
}
```
When using a local checkout before publishing, run:
```bash
./gradlew publishToMavenLocal
```
Then use `mavenLocal()` in the consuming Gradle project.
## Package Map
| Package                                     | Purpose                                                             |
|---------------------------------------------|---------------------------------------------------------------------|
| `vvvfsimulator.data.vvvf`                   | Load, save, clone, and inspect VVVF strategy YAML data.             |
| `vvvfsimulator.data.basefrequency`          | Define speed/frequency timelines for offline generation.            |
| `vvvfsimulator.data.trainaudio`             | Configure motor, gear, harmonic, filter, and impulse-response data. |
| `vvvfsimulator.vvvf.model`                  | Runtime motor and PWM model structures.                             |
| `vvvfsimulator.vvvf.calculation`            | Low-level phase-state and modulation calculation.                   |
| `vvvfsimulator.vvvf.modulation`             | Carrier, custom PWM, SVM, and delta-sigma helpers.                  |
| `vvvfsimulator.generation.audio`            | Shared realtime and offline audio-generation parameters.            |
| `vvvfsimulator.generation.audio.vvvfsound`  | VVVF waveform audio export and realtime stepping.                   |
| `vvvfsimulator.generation.audio.trainsound` | Train running sound, impulse responses, and convolution helpers.    |
| `vvvfsimulator.audiofilter`                 | FFT and convolution primitives.                                     |
| `loader`                                    | Shared load result and error enums.                                 |
## Examples
### Loading VVVF YAML
Use `vvvfsimulator.data.vvvf.Manager.load` to read a VVVF-Simulator YAML config
from any `InputStream`. The loader reads UTF-8 text and updates the global
`Manager.current` value on success.
```java
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import loader.LoadContext;
import loader.LoadException;
import vvvfsimulator.data.vvvf.Manager;
import vvvfsimulator.data.vvvf.Struct;
public class YamlLoader{
    public static void load(){
        Path yamlPath=Path.of("your_config.yaml");
        try(InputStream in=Files.newInputStream(yamlPath)){
            LoadContext context=Manager.load(yamlPath.toString(),in);
            if(context.exception!=LoadException.normal)
                throw new IllegalArgumentException(
                        "Failed to load YAML: "+context.exception+
                                " at "+context.row+":"+context.col);
        }
        Struct strategy=Manager.deepClone(Manager.current);
    }
}
```
#### YAML Load Results
`LoadContext.exception` can contain:

| Value     | Meaning                                                             |
|-----------|---------------------------------------------------------------------|
| `normal`  | The YAML was loaded successfully.                                   |
| `empty`   | SnakeYAML returned no document, usually from an empty input.        |
| `io`      | An `IOException` occurred while reading.                            |
| `lex`     | SnakeYAML scanner error.                                            |
| `parse`   | SnakeYAML parser error.                                             |
| `compose` | SnakeYAML composer error.                                           |
| `dump`    | Duplicate key error.                                                |
| `init`    | Constructor error, unsupported value, or another SnakeYAML failure. |
For SnakeYAML syntax errors, `LoadContext.row` and `LoadContext.col` contain the
reported problem position. They come from SnakeYAML's `Mark` and are zero-based.
#### Compatibility Fixes
The YAML loader preserves compatibility with several naming differences:
- `Saw` is accepted as `Triangle` for base-wave and harmonic types.
- `ModifiedSaw1` is accepted as `ModifiedTriangle1`.
- `ΔΣ` is accepted as `DELTA_SIGMA`.

Unknown enum values fall back to each field's existing default instead of crashing the loader.
#### Global Manager State
`Manager.current`, `Manager.loadData`, and `Manager.loadPath` are static global
state. This is convenient for simple tools, but for library-style applications
you should copy the data after loading:
```java
Struct immutableSnapshot=Manager.deepClone(Manager.current);
```
Use `Manager.resetCurrent()` before loading another independent config if your
application reuses the same JVM for many tasks.
### Saving VVVF YAML
Use `Manager.save` or `Manager.saveCurrent` to write YAML back to disk.
```java
import vvvfsimulator.data.vvvf.Manager;
import vvvfsimulator.data.vvvf.Struct;
public class YamlSaver{
    public static void save(){
        Struct strategy=Manager.deepClone(Manager.current);
        boolean saved=Manager.save("your_config.yaml",strategy);
        if(!saved) throw new IllegalStateException("Failed to save YAML");
    }
}
```
To throw an exception instead of returning `false`:
```java
import vvvfsimulator.data.vvvf.Manager;
import vvvfsimulator.data.vvvf.Struct;
public class YamlSaver{
    public static void save(){
        Struct strategy=Manager.deepClone(Manager.current);
        boolean saved=Manager.save("your_config.yaml",strategy,true);
    }
}
```
### Train Audio Data
Train running sound is configured with `vvvfsimulator.data.trainaudio.Struct`.
The default constructor creates a usable default motor specification and default gear harmonics.
```java
import vvvfsimulator.data.trainaudio.Struct;
public class TrainAudioData{
    public static void generate(){
        Struct trainSound=new Struct();
        trainSound.totalVolumeDb=0.0;
        trainSound.motorVolumeDb=-2.0;
        trainSound.useConvolutionFilter=true;
        // Optional: regenerate gear harmonics from gear tooth counts.
        trainSound.setCalculatedGearHarmonic(16,101);
    }
}
```
The most commonly adjusted fields are:

| Field                       | Meaning                                                            |
|-----------------------------|--------------------------------------------------------------------|
| `motorSpec`                 | Motor electrical and mechanical parameters.                        |
| `gearSound`                 | Gear harmonic definitions.                                         |
| `harmonicSound`             | Extra harmonic sounds.                                             |
| `filters`                   | Peaking EQ, high-pass, low-pass, and notch filter settings.        |
| `useFilters`                | Enables filter processing in consumers that support it.            |
| `useConvolutionFilter`      | Enables impulse-response convolution in consumers that support it. |
| `impulseResponse`           | IR samples used by convolution.                                    |
| `impulseResponseSampleRate` | Sample rate of `impulseResponse`.                                  |
| `motorVolumeDb`             | Motor volume in dB.                                                |
| `totalVolumeDb`             | Final output volume in dB.                                         |
### Loading Impulse Responses
Use `AudioResourceManager.load` to load impulse-response audio from an
`InputStream`. It updates the global `AudioResourceManager.ir` and
`AudioResourceManager.ir_sample_rate` values on success.
```java
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import loader.LoadContext;
import loader.LoadException;
import vvvfsimulator.generation.audio.trainsound.AudioResourceManager;
public class IRLoader{
    public static void load(){
        Path path=Path.of("your_ir.ir");
        boolean isIr=path.toString().endsWith(".ir");
        try(InputStream in=Files.newInputStream(path)){
            LoadContext context=AudioResourceManager.load(in,isIr);
            if(context.exception!=LoadException.normal){
                throw new IllegalArgumentException("Failed to load IR: "+context.exception);
            }
        }
        double[] response=AudioResourceManager.resampleLinear(44100);
    }
}
```
#### Supported IR Formats
`AudioResourceManager.load(stream, true)` expects the library's compact `.ir` format:
- 4-byte magic: `IR\0\0`
- unsigned 32-bit little-endian sample rate
- mono little-endian `float32` samples

`AudioResourceManager.load(stream, false)` expects WAV data. The WAV loader supports:
- `RIFF`, `RIFX`, and `RF64` containers with `WAVE` format.
- PCM integer samples.
- IEEE float samples.
- Multichannel audio, mixed down to mono by averaging channels.

Unsupported or invalid audio resets the loaded IR to `{1.0}` and sets `ir_sample_rate` to `-1`.
#### IR Load Results
| Value      | Meaning                                                        |
|------------|----------------------------------------------------------------|
| `normal`   | The IR or WAV was decoded successfully.                        |
| `io`       | The stream could not be read.                                  |
| `irerror`  | The `.ir` header, sample rate, or sample data was invalid.     |
| `waverror` | The WAV container or sample format was invalid or unsupported. |
### Offline WAV Generation
Use `vvvfsimulator.generation.audio.vvvfsound.Audio` to export generated VVVF sound to a WAV file.
```java
import vvvfsimulator.data.basefrequency.Manager;
import vvvfsimulator.data.basefrequency.StructCompiled;
import vvvfsimulator.data.trainaudio.Struct;
import vvvfsimulator.generation.GenerateCommon.GenerationParameter;
import vvvfsimulator.generation.audio.vvvfsound.Audio;
public class WavGenerator{
    public static void generate(){
        vvvfsimulator.data.vvvf.Struct strategy=vvvfsimulator.data.vvvf.Manager.deepClone(
                vvvfsimulator.data.vvvf.Manager.current);
        Struct trainSound=new Struct();
        StructCompiled timeline=Manager.getTemplate().getCompiled();
        GenerationParameter parameter=new GenerationParameter(timeline,strategy,trainSound);
        Audio.exportWavLine(parameter,44100,"line.wav");
        Audio.exportWavPhaseCurrent(parameter,44100,"phase_current.wav");
    }
}
```
`GenerationParameter.progress` is updated while exporting:
- `progress.total`: estimated sample count.
- `progress.progress`: samples already generated.
- `progress.cancel`: set to `true` from another thread to stop generation.

The WAV exporter writes mono 16-bit PCM and patches the WAV data size when
generation completes or is canceled.
### Realtime State Stepping
`vvvfsimulator.generation.audio.RealTime` contains shared state and frequency
control helpers for realtime engines.
```java
import vvvfsimulator.data.trainaudio.Struct;
import vvvfsimulator.generation.audio.RealTime;
import vvvfsimulator.data.vvvf.Analyze;
import vvvfsimulator.vvvf.calculation.Common;
import vvvfsimulator.vvvf.model.Struct.PhaseState;
public class Engine{
    public static void update(){
        vvvfsimulator.data.vvvf.Struct strategy=vvvfsimulator.data.vvvf.Manager.deepClone(
                vvvfsimulator.data.vvvf.Manager.current);
        Struct trainSound=new Struct();
        RealTime.VvvfSoundParameter parameter=new RealTime.VvvfSoundParameter(strategy,trainSound);
        double sampleRate=44100.0;
        double dt=1.0/sampleRate;
        parameter.frequencyChangeRate=8.0;
        parameter.isBraking=false;
        parameter.isFreeRunning=false;
        for(int i=0;i<1024;i++){
            int result=RealTime.realTimeFrequencyControl(parameter.control,parameter,dt);
            if(result!=-1) break;
            Analyze.calculate(parameter.control,parameter.vvvfSoundData);
            parameter.control.addTimeAll(dt);
            PhaseState state=Common.calculatePhaseState(parameter.control,0.0);
            double sample=(state.u-state.v)*0.5;
            // Send sample to your audio device, ring buffer, file writer, etc.
        }
    }
}
```
The provided `vvvfsimulator.generation.audio.vvvfsound.RealTime.calculate` and
`vvvfsimulator.generation.audio.trainsound.RealTime.generate` methods are simple
state-loop references. They do not expose an output callback, so applications
that need actual realtime playback should use the lower-level pattern shown above.
### Train Sound Calculation
The train sound helpers calculate mechanical motor, gear, and harmonic sounds
from the current VVVF domain state.
```java
import vvvfsimulator.data.trainaudio.Struct;
import vvvfsimulator.generation.audio.trainsound.Audio;
import vvvfsimulator.vvvf.model.Struct.Domain;
public class TrainAudio{
    public static void generate(){
        Struct trainSound=new Struct();
        Domain domain=new Domain(trainSound.motorSpec);
        double trainSample=Audio.calculateTrainSound(domain,trainSound);
    }
}
```
Useful methods:

| Method                                                    | Purpose                                                             |
|-----------------------------------------------------------|---------------------------------------------------------------------|
| `Audio.calculateHarmonicSounds(domain, harmonics)`        | Calculates harmonic lists directly.                                 |
| `Audio.calculateTrainSound(domain, data)`                 | Calculates running sound and motor state.                           |
| `Audio.calculateTrainSoundFromCurrentState(domain, data)` | Uses the current domain state without advancing some derived state. |
### Convolution and Filters
Use `AudioFilter.CppConvolutionFilter` for block-based impulse-response convolution.
```java
import vvvfsimulator.generation.audio.trainsound.AudioFilter.CppConvolutionFilter;
public class FFTConv{
    public static void convolute(){
        int blockSize=1024;
        double[] response={1.0};
        CppConvolutionFilter filter=new CppConvolutionFilter(blockSize, response);
        double[] block=new double[blockSize];
        filter.process(block,block.length);
        // For shared kernels used by many filter instances
        CppConvolutionFilter.updateSharedResponse(blockSize, response);
    }
}
```
Stereo utility methods are also available:
- `stereo2monaural(input, len, outputL, outputR)`
- `monaural2stereo(inputL, inputR, output, len)`
### Low-Level PWM Calculation
For applications that need direct waveform inspection instead of audio output,
use the calculation package directly.
```java
import vvvfsimulator.data.vvvf.Analyze;
import vvvfsimulator.data.trainaudio.Struct;
import vvvfsimulator.vvvf.calculation.Common;
import vvvfsimulator.vvvf.model.Motor;
import vvvfsimulator.vvvf.model.Struct.Domain;
import vvvfsimulator.vvvf.model.Struct.PhaseState;
public class PWMCalc{
    public static void calculate(){
        vvvfsimulator.data.vvvf.Struct strategy=vvvfsimulator.data.vvvf.Manager.deepClone(
                vvvfsimulator.data.vvvf.Manager.current);
        Struct trainSound=new Struct();
        Motor.MotorSpecification motorSpec=trainSound.motorSpec;
        Domain domain=new Domain(motorSpec);
        domain.setControlFrequency(20.0);
        domain.setBaseWaveFrequency(20.0);
        Analyze.calculate(domain,strategy);
        PhaseState state=Common.calculatePhaseState(domain,0.0);
        double lineVoltage=state.u-state.v;
    }
}
```
`Common.getCalculator(level, pulseType)` selects the L2 or L3 phase-state
calculator according to the PWM level. `L2.getCalculator` and `L3.getCalculator`
are available when you need to choose the implementation explicitly.
### Data Copying and Thread Safety
Several manager classes keep static global state for convenience:
- `vvvfsimulator.data.vvvf.Manager.current`
- `vvvfsimulator.data.basefrequency.Manager.current`
- `vvvfsimulator.data.trainaudio.Manager.current`
- `AudioResourceManager.ir`

This style is simple for single-file tools, but shared applications should avoid
mutating these globals while another thread is generating audio. Prefer this
pattern:
1. Load or configure data on a setup thread.
2. Clone data with `deepClone` or `copy`.
3. Pass immutable snapshots to audio-generation workers.
4. Replace snapshots atomically when reloading configs.
### Error Handling Pattern
The library usually reports load failures through `LoadContext` instead of
throwing. A compact helper pattern is:
```java
import loader.LoadContext;
import loader.LoadException;
public class ContextHelper{
    static void requireNormal(LoadContext context,String what){
        if(context.exception!=LoadException.normal){
        throw new IllegalArgumentException(what+" failed: "+context.exception
                +" at "+context.row+":"+context.col);
        }
    }
}
```
Use it with both YAML and impulse-response loading.
### Building From Source
From the repository root:
```bash
./gradlew build
```
To publish to the local Maven cache:
```bash
./gradlew publishToMavenLocal
```
Generated artifacts include:
- Main jar
- Sources jar
- Javadoc jar
### Migration Notes
If you are migrating code from a Minecraft mod:
- Replace Minecraft resource loading with ordinary `InputStream` loading.
- Keep Minecraft config screens, resource-pack discovery, networking, and mod
  lifecycle code outside this core library.
- Treat this library as the calculation and data layer only.
- If multiple Minecraft mods need this library at runtime, wrap this core jar in
  a separate shared-library mod and let those mods depend on that wrapper.
### Minimal End-to-End Example
```java
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import loader.LoadContext;
import loader.LoadException;
import vvvfsimulator.data.basefrequency.StructCompiled;
import vvvfsimulator.data.trainaudio.Struct;
import vvvfsimulator.generation.GenerateCommon.GenerationParameter;
import vvvfsimulator.generation.audio.vvvfsound.Audio;
public class ExportExample {
    public static void main(String[] args) throws Exception{
        Path yaml=Path.of("your_config.yaml");
        try(InputStream in=Files.newInputStream(yaml)){
            LoadContext context=vvvfsimulator.data.vvvf.Manager.load(yaml.toString(),in);
            if(context.exception!=LoadException.normal){
                throw new IllegalArgumentException("YAML error: "+context.exception);
            }
        }
        vvvfsimulator.data.vvvf.Struct strategy=vvvfsimulator.data.vvvf.Manager.deepClone(
                vvvfsimulator.data.vvvf.Manager.current);
        Struct trainSound=new Struct();
        StructCompiled timeline=
                vvvfsimulator.data.basefrequency.Manager.getTemplate().getCompiled();
        GenerationParameter parameter=new GenerationParameter(timeline,strategy,trainSound);
        Audio.exportWavLine(parameter,44100,"output.wav");
    }
}
```