/* Please ensure you have read 'README.pdf' before continuing */
(
/* Boot the server if it is not already running or booting. If the server is already running, execute immediately */
Server.default.waitForBoot({
  var colourArray, stepDuration, numberOfSteps, samplesForRowsDirectory, killSamplesDirectory, metronome;

  /* The colours for 'GeneticDrummer': 'background', 'buttonOn', 'stepOn', 'buttonStepOn' */
  colourArray = ["#B5B2B1", "#00CC66", "#80E6B2", "#9933FF"];

  /* The between the steps of a sequence */
  stepDuration = 0.25;

  /* The number of steps in a sequence */
  /* N.B. This variable has been designed to automatically scale. Anything beyond 100 (although possible - try it!)
  starts to get a bit silly */
  numberOfSteps = 16;

  /* Set whether or not you wish to hear a metronome click on every 1st beat */
  metronome = false;

  /* Set the sammples that you wish to use for the step sequencer; this is defaulted to look in the extensions
  directory */
  /* 'GeneticDrummer' loads these samples in ascending order. If you wish to have order in the resulting rows, name
  the samples: '1_someName', '2_someName', '3_someName', etc... */
  /* 'GeneticDrummer' is designed to build the correct number of rows to match the number of given samples. On a 13"
  screen, anything above 16 samples will not fit. */
  samplesForRowsDirectory = Platform.userExtensionDir +/+ "GenerativeCreativity/Samples/Drum Samples/*.wav";

  /* Set the sammples that you wish to use for the for the 'kill' sounds; this is defaulted to look in the extensions
  directory */
  /* N.B. The 'kill' sounds may start to send you insane (sorry). In the event of this entirely inevitable scenario,
  please do not panic. Simply set the 'out' argument of '\killSoundsPlayBuf' to a channel not being used (normally
  anything above 8). Or if you are feeling adventerous: record your own 'kill' sounds (.wavs) and load them in instead
  - much more entertaining! */
  killSamplesDirectory = Platform.userExtensionDir +/+ "GenerativeCreativity/Samples/Death Samples/*.wav";

  /* Create a SynthDef instance '\killSoundsPlayBuf'; evaluate the ugenGraphFunc; build the ugenGraph */
  SynthDef(\killSoundsPlayBuf, {
    arg bufNum = 0, out = 0;

    Out.ar(out, PlayBuf.ar(1, bufNum, 1, doneAction:2));
  }).add; Server.default.sync;

  /* Create a SynthDef instance '\StepRowPlayBuf'; evaluate the ugenGraphFunc; build the ugenGraph */
  SynthDef(\StepRowPlayBuf, {
    arg bufNum = 0, trig = -1;
    Out.ar(0, PlayBuf.ar(1, bufNum, 1, trig, 0, 0));
  }).add; Server.default.sync;

  /* Finally, create a 'GeneticDrummer' with the arguments specified above passed in */
  GeneticDrummer(colourArray, stepDuration, numberOfSteps, samplesForRowsDirectory, killSamplesDirectory, metronome)
});
)