/*
* HeartRateMonitor.sc
* Purpose: A class that is used retrieve information that is being sent over OSC
*
* @author: Jack Clark
* @version: 1.0
* @info: Built in SuperCollider 3.6
* @line length limit: 120
*/

HeartRateMonitor {
  /* Global Variables; variables that are required to be accessible in every scope
  N.B. 'heartRateData' is a 'getter'; allows the retrurn of its value  */
  <heartRateData;

  /*
  * Create a new instance of 'HeartRateMonitor'
  *
  * @return: A HeartRateMonitor
  */
  *new {
    /* Invoke 'new' on the superclass; return a new object; invoke the instance method 'initialse' on returned object */
    ^super.newCopyArgs.initialse();
  }

  /*
  * Initialse 'GeneticDrummerRow', seting appropriate conditions;.
  */
  initialse {
    /* Invoke 'createOscDef' */
    this.createOscDef;
  }

  /*
  * Creates an OSC definition for retrieving messages */
  */
  createOscDef {
    /* Although it would be possible to not assigning the OSCdef to a variable
    I have chosen to do so to allow for access to its class methods */
    OSCdef(\heartRateData, {
      arg incomingMessage;

      /* Set the local variable 'heartRateData' to be equal to that of the
      message that of our incoming OSC message */
      heartRateData = incomingMessage[1];

      /* 'if' we have set the 'asTempo' boolean to true */
      if(asTempo == true) {
        /* Set the new value as the tempo */
        this.useAsTempo(heartRateData);
        //"I am the tempo".postln;
      };

    }, '/heartRate')
  }

  /* Simple method for setting the permanent default TempoClock instantiated when
  the server starts up. This method uses 'tempo' which sets the clocks beats per
  second */
  useAsTempo {
    arg aHeartRate;

    try {
      TempoClock.default.tempo = aHeartRate.linexp(60, 180, 60, 200) / 60;
    } {
      // If we need the caught error message
      arg error;

      "".postln;
      "ERROR CAUGHT --> Not able to set tempo as 'heartRateData' is 'nil'".postln;
    }
  }
}