![Flow Splash Banner](docs/web/Banner.png)


# Flow  
A Polyphonic, Multitimbral, Modular Additive Synthesizer (Version 12)
 
By Sean Luke (sean@cs.gmu.edu) \
With Help from V. Hoyle \

Related projects:  

* [Edisyn](https://github.com/eclab/edisyn), a patch editor toolkit with sophisticated exploration tools.
* [Gizmo](https://cs.gmu.edu/~sean/projects/gizmo/), an Arduino-based MIDI Swiss Army knife.
* [Seq](https://github.com/eclab/seq), a very unusual hierarchical and modular music sequencer.
* [Arduino Firmware](https://github.com/eclab/grains) (oscillators, modulators, etc.) for the AE Modular Grains module.  Includes an ultralight but full-featured [MIDI library for small microcontrollers](https://github.com/eclab/grains/tree/main/midi).
* [*Computational Music Synthesis*](https://cs.gmu.edu/~sean/book/synthesis/), an open-content book on building software synthesizers.

## Donations

Donations are welcome via Paypal to my email address (sean@cs.gmu.edu).

## About

Flow is a fully-modular multitimbral and polyphonic additive software synthesizer written in pure Java.  It runs on MacOS, Linux, and Windows.  I have used it to play individual patches and to play many simultaneous patches for a full song controlled over MIDI via a DAW.

Flow has almost 70 modules of different shapes and sizes, and currently supports up to 32 voices at up to 256 partials and 44.1KHz with a rate of one new partial update every 32 samples.  Flow is a very computationally expensive program and will keep your laptop quite warm and your fan busy.  You need to have some fairly good hardware to run Flow at full blast successfully (for reference, Flow was developed on a 2.8Ghz i7 2015 Macbook Pro Retina).  There are options for reducing Flow's footprint (such as reducing the number of voices or partials).

## Resources

* Flow has an [extensive manual](https://cs.gmu.edu/~eclab/projects/flow/flow.pdf) which discusses additive synthesis, Flow's modular approach, how to run it, how to integrate it with controllers and DAWs, and how to build new modules.

* One of Flow's modules is a wavetable synth.  It's designed to load wavetables built with [WaveEdit](http://synthtech.com/waveedit/), and there are quite a number to choose from on their associated website, [WaveEditOnline](https://waveeditonline.com/)

* Another of Flow's modules lets you load, draw, and save out partials.  This module can load single-cycle waves such as [Adventure Kid's Single Cycle Waveforms](https://www.adventurekid.se/akrt/waveforms/).

* Interested in helping out on Flow?  Our [To Do list](TODO.md) lists current open issues and bugs.

### Demos
* A [video demonstrating how to set up a simple patch in Flow](https://youtu.be/zkmEVWfly-0).

* Three songs made using only Flow in multitimbral mode, with Ableton serving as just the sequencer.  They are called [8](https://cs.gmu.edu/~sean/projects/synth/log/#8), [9](https://cs.gmu.edu/~sean/projects/synth/log/#9), and [10](https://cs.gmu.edu/~sean/projects/synth/log/#10).  8 is the best one. All three come with the Flow patches and Ableton files to recreate them.

* One song using Flow in multitimbral mode with [Seq](https://github.com/eclab/seq) serving as the sequencer.  It's called [14](https://cs.gmu.edu/~sean/projects/synth/log/#14).  It's nothing special, it was written to stress-test Seq.

### Patches

* Flow's demo patches are located [here](flow/patches).   Contribute some!   You can get a zip file of all of them [here](https://cs.gmu.edu/~eclab/projects/flow/patches.zip).

## Install and Run Flow

Flow is cross-platform and will run on a variety of platforms (Windows, Linux) but we are personally developing on and for MacOS and Linux. We'd appreciate feedback and screenshots of it running on Windows so we can tweak things.


### Installation and Running on MacOS

First install Flow from this link: [Flow.dmg](https://cs.gmu.edu/~eclab/projects/flow/Flow.dmg). 
Sadly, it's a whopping 50MB because it includes the Java VM.  :-(

You'll also want to download some [patches](https://cs.gmu.edu/~eclab/projects/flow/patches.zip), some [wavetables](https://waveeditonline.com/), and the [manual](https://cs.gmu.edu/~eclab/projects/flow/flow.pdf).  Pay attention to section 2.1 of the manual, where it explains how to tune Flow for your computer speed.  

MacOS has lately locked down the ability to run an application that's not from a commercial, paying Apple Developer.  And GMU is not one.  So you will have to instruct Sierra to permit Flow to run.

#### Installing under MacOS X Prior to Sequoia

This is pretty easy. CONTROL-Click on Flow's App Icon, and while holding Control down, select "Open".  Now instead of telling you that Flow cannot be opened because it's from an unidentified developer, you'll be given the option to do so. You probably will only have to do this once.

#### Installing under MacOS X Sequoia and Later

Apple has made this much more annoying now, to everyone's consternation.  You'll have to use the Terminal program.  Let's assume you stuck Flow in the /Applications directory as usual.  Now we have to tell Gatekeeper to allow Flow to run on your machine:

1. Run the Terminal Program (in /Applications/Utilities/)
2. Type the following command and hit RETURN: `   sudo xattr -cr /Applications/Flow.app`
4. Enter your password and hit RETURN
5. Quit the Terminal Program

Now you should be able to run Flow.  You only have to do this once.  This should work with earlier versions of OS X too. 

If you want to use Flow in combination with a DAW, see the manual's section on doing that.

You can also run Flow from its jar file from the command line: see "Running from the command line" at end of these instructions. 

#### Notes on Catalina and later...

* Flow cannot access your laptop's microphone on Catalina and later.  I don't know if it can access external microphones.

#### Rosetta and the M1

At present Flow only runs under Rosetta on the M1, because its package contains an Intel-only Java VM (because I ownly own an Intel Mac and am not running Big Sur).  Don't expect things to change until I get an M1.  If you have installed Java yourself, you can run Flow from the command line (see later below) and it'll probably run natively.




### Installation and Running on Windows

The following should work (but has not been tested):

1. [Download and install at least Java 20](https://www.oracle.com/technetwork/java/javase/downloads).  The JRE should work fine.  Earlier versions of Java have a bug which causes Java apps (like Flow) to make teeny tiny windows on the latest high-resolution screens.

2. Download Flow's jar file, called [flow.jar](https://cs.gmu.edu/~eclab/projects/flow/flow.jar).

3. You'll also want to download some [patches](https://cs.gmu.edu/~eclab/projects/flow/patches.zip), some [wavetables](https://waveeditonline.com/), and the [manual](https://cs.gmu.edu/~eclab/projects/flow/flow.pdf).  Pay attention to section 2.1 of the manual, where it explains how to tune Flow for your computer speed.

4. Double-click on flow.jar to launch Flow.

5. See **Running from the command line** below for more glitch-free options.

#### Note

Flow makes heavy use of Java preferences.  There is a longstanding Java/Windows bug which breaks Java preferences and will cause Flow to be unable to make any of your preferences persistent.  As of Java 11 the bug should be fixed, but if it's not, please let us know.


### Installation and Running on Linux

Flow should work fine if you have installed at least *Java 20*.

1. Install at least Java 20 (openjdk).

2. Download Flow's jar file, called [flow.jar](https://cs.gmu.edu/~eclab/projects/flow/flow.jar).

3. You'll also want to download some [patches](https://cs.gmu.edu/~eclab/projects/flow/patches.zip), some [wavetables](https://waveeditonline.com/), and the [manual](https://cs.gmu.edu/~eclab/projects/flow/flow.pdf).  Pay attention to section 2.1 of the manual, where it explains how to tune Flow for your computer speed.

4. You'll need to figure out how to make it so that double-clicking on the jar file launches it in java.  In Ubuntu, here's what you do: right-click on the jar file icon and choose "Properties".  Then select the "Open With" tab, and select your Java VM (for example "Open JDK Java 8 Runtime").  The press "Set as Default".  This makes the Java VM the default application to launch jar files.

5. Thereafter you should be able to just double-click on the file to launch Flow.

6. See **Running from the command line** below for more glitch-free options.


### Running from the command line (MacOS, Windows, Linux)

1. Make sure Java 20 or later installed.

2. Download Flow's jar file, called [flow.jar](https://cs.gmu.edu/~eclab/projects/flow/flow.jar).

3. Grab some [patches](https://cs.gmu.edu/~eclab/projects/flow/patches.zip), some [wavetables](https://waveeditonline.com/), and the [manual](https://cs.gmu.edu/~eclab/projects/flow/flow.pdf).  

4. Run Flow as:   `java -jar flow.jar`

5. Flow benefits from a lower-latency garbage collector to prevent it from glitching.  You might instead run Flow as:     `java -jar flow.jar -XX:+UseZGC -XX:MaxGCPauseMillis=1`



