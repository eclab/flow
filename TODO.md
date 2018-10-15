To Do
=====

This is a list of stuff that's not done yet.  Pick something and work on it!


System
------

* We need a more convenient mechanism for testing new modules rather than adding 
  their classname to the AppMenu class and recompiling it.

* Midi Devices may have the same name in Linux; for example, the two incoming
  MIDI devices from the Korg Microsampler.  We need to detect this and use other
  info (version etc.) to distinguish them.

* The software has not been tested on Windows at all, and only in limited form on 
  Linux.

* Modulation should revert to the previous constant value, rather than the default, after you've pulled the plug on a wire.

* Must figure out why the voice thread slows DOWN when only outputting a
  single voice (see comment around line 400 of Output.java)

* I don't know why the various voices get slightly out of sync with one
  another.  They should be in perfect sync.  

* simpleSort is (right now) a Cocktail Sort -- a variation of Bubble Sort.  The
  idea is to have a good sort for arrays that have very little out of place, and
  almost always require small swaps.  This happens to be the one and only compelling use case
  for Bubble Sort ever.  But perhaps we should replace this with insertion sort anyway.  
  
* MIDI clock is flakey and inaccurate.  Also, we ignore clock pulses while stopped,
  when in fact we should be using them to estimate our upcoming tempo.

* We need a main() that fires up officially.  This is basically waiting until we
  can come up with a name.
  
* Ultimately we're going to need a better patch file format.  At the moment we use
  Java serialization.  The problem with this is that when we add new features to
  modules which change their number or type of instance variables, it'll break every
  patch which uses that module.  Some kind of version handling would be helpful. 
  
* Is there a strong reason to add back in Phase?

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
* AmpMath
* Average
* Buffer
* Combine
  - We might disregard zero-amplitude partials
* Compress
  - This doesn't feel like a very useful module.  Perhaps it should be eliminated.
  - Or maybe merge it in with AmpMath
  - We need to consider what to do with amplitudes >= 1.0
* DADSR
  - Has not been tested with MIDI Sync yet
  - Levels and times in Envelope are fixed on Gate.  
* Delay
* Draw
* Drawbars
* Envelope
  - Lots of problems.  Sustain and sustain loop, actually all modes don't work right, lots of values appear to be wrong.
  - Has not been tested with MIDI Sync yet
  - Curves are linear.  We could add nonlinear curves as in DADSR.
  - Levels and times in Envelope are fixed on Gate.  
* Fatten
* Fill
* Filter
  - Filter needs a lot of work: the filters are not resonant.
* FlangeFilter
  - What do we need to add to this to have better chorusing, and flanging?  These sound bad now.  The comb filter partials are only approximate.
  - Add phasing options?
  - Needs defaults  
* Harmonics
  - Need to better organize the existing harmonics and weed out the dumb ones
* In
* Jitter
* LFO
  - Has not been tested with MIDI Sync yet
  - Gate doesn't seem to reset quite to the right spot
* LinearFilter
  - Frequencies may be too sensitive.
* Macro
  - Needs to be more compact in its GUI display somehow
* Map
  - In addition to min/max, it'd be useful to have a center and variance
* MIDIIn
  - Not adequately tested
  - Is MIDI Clock working?
  - CC may not work yet, not sure
  - What else should we include?
* Mix
* ModMath
* Morph
  - Right now we're doing simpleSort.  Test to see if we should *always*
    do bigSort, especially in the random morphs.
  - Except Fundamental may not work
* Noise
  - Defaults
  - Not intuitive how to merge noise with other sound sources (using Fill)
  - Reducing (not increasing) the number of partials in Noise creates weird pops.  
    Not sure why.
  - This module could be expanded on considerably
* NRPN
  - Not tested yet
* Out
  - Should we move Gain to above the modulations?
  - Should we add compression/limiting?
* PartialMod
  - Seems not to be working.
* PartialFilter
* Shift
* Rand
  - Is this a useful module?  Should we delete or merge it?
* Rectified
* Sample and Hold
* Sawtooth
* Scale
* Sine
  -  This could be made more useful perhaps
* Skeletonize
* Square
* Squish
* Stretch
* Swap
* Tinkle
* Triangle
* VCA
* Viewer
  - This needs to be cleaned up a bit
* Wave
  - I asked nicely to Waldorf to use their waves (which I have) and they said no.
    So we need a new source of wavetable waves.
* Wavetable
  - I asked nicely to Waldorf to use their wavetables (which I have) and they said no.
    So we need a new source of wavetables.

Uncompleted Modules
-------------------

* Bell
  - I'm working on a bell module with independent envelopes and proper harmonics.  It's not close to done yet.

* Constrained
  - This module does not exist. The idea should be to have a module which does more
    advanced combinations of constraints than you'd have in the standard constraint
    facility.  Should we make one?

* PartialsLab
  - I'm working on an additional function-based partials source.  Also not done.

