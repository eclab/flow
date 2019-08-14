To Do
=====

This is a list of stuff that's not done yet.  Pick something and work on it!


System
------

* Midi Devices may have the same name in Linux; for example, the two incoming
  MIDI devices from the Korg Microsampler.  We need to detect this and use other
  info (version etc.) to distinguish them.

* The software has not been tested on Windows at all, and only in limited form on 
  Linux.

* MIDI clock is flakey and inaccurate.  Also, we ignore clock pulses while stopped,
  when in fact we should be using them to estimate our upcoming tempo.

* At present we're using java.util.Random, which is (1) multithreaded and thus
  unneccessarily slow, and (2) very bad quality.  The quality doesn't really
  matter for our purposes, and I'm keeping it to make it easier for people
  unused to other generators.  But I've included a rudimentary XORShift32
  implementation which should be fine for our purposes and is also much faster
  and somewhat better quality.  Perhaps we should move to it?
  
* **Overall: we need compelling patches.**  I think right now the modules have a lot
  of promise but they still feel meh.  It's fun to experiment in additive, but
  we need patches that are nontrivial to do in some other way and are useful.

Modules
-------

* All
* AHR
* AmpMath
* Average
* Buffer
* Choice
* Combine
* Compress
  - This doesn't feel like a very useful module.  Perhaps it should be eliminated.
  - Or maybe merge it in with AmpMath
  - We need to consider what to do with amplitudes >= 1.0
* DADSR
* Delay
* Dilate
* Draw
* Drawbars
* EitherOr
* Envelope
* Fatten
* Fill
* Filter
* Fix
* FlangeFilter
* Geiger
* Harmonics
  - Need to better organize the existing harmonics and weed out the dumb ones
* In
* Jitter
* LFO
* LinearFilter
  - Frequencies may be too sensitive.
* Macro
* Map
* MIDIIn
* Mix
* ModMath
* Morph
* MPE
* Noise
* Normalize
* Note
* NRPN
* Out
* PartialMod
  - Seems not to be working right.
* PartialFilter
* PartialsLab
  - We could extend this to make it more useful.
* PartialMod
* Partials
* Rand
  - Is this a useful module?  Should we delete or merge it?
* Rectified
* Rotate
* Sample and Hold
* Sawtooth
* Scale
* Seq
  - This is incomplete and not working properly.  Curve has no purpose.
* Shift
* Sine
  -  This could be made more useful perhaps
* Skeletonize
* Smooth
* Soften
* Square
* Squish
* Stretch
* Sub
* Swap
* Tinkle
* Triangle
* User
* VCA
* Wavetable


Uncompleted Modules
-------------------

* Bell
  - I'm working on a bell module with independent envelopes and proper harmonics.  It's not close to done yet.

* Constrained
  - This module does not exist. The idea should be to have a module which does more
    advanced combinations of constraints than you'd have in the standard constraint
    facility.  Should we make one?

