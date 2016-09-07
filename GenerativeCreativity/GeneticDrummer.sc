/*
* GeneticDrummer.sc
* Purpose: A biologically inspired drum machine!
*
* @author: Jack Clark
* @version: 1.0
* @info: Built in SuperCollider 3.6
* @line length limit: 120
*/

GeneticDrummer {
  /*
  * Create a new instance of 'GeneticDrummer'
  *
  * @param parentWindow: The parent window for this object
  *
  * @return: A GeneticRowDrummer
  */
  *new {
    /* Arguments passed into 'GeneticDrummerRow' upon creation */
    arg colourArray, stepDuration, numberOfSteps, samplesForRowsDirectory, killSamplesDirectory, metronome;

    /* Invoke 'new' on the superclass; return a new object; invoke the instance method 'initialse' on returned
    object */
    ^super.new.initialse(colourArray, stepDuration, numberOfSteps, samplesForRowsDirectory, killSamplesDirectory,
      metronome)
  }

  /*
  * Initialse 'GeneticDrummerRow', seting the appropriate conditions;.
  *
  * @params: See above
  */
  initialse {
    arg colourArray, stepDuration, numberOfSteps, samplesForRowsDirectory, killSamplesDirectory, metronome;
    var background, buttonOn, stepOn, buttonStepOn, window, samples, killSamples, geneticDrummerRows;

    /* Unpack 'colourArray' into its seperate colors */
    # background, buttonOn, stepOn, buttonStepOn = this.setColours(colourArray);

    /* Create a window with a default width and height; these will automatically regardless */
    window = this.createApplicationWindow(1120, 600);

    /* Set 'samples' to be those passed */
    samples = this.setSamples(samplesForRowsDirectory);
    /* Set 'KillSamples' to be those passed */
    killSamples = this.setSamples(killSamplesDirectory);

    /* For each sample collect... */
    geneticDrummerRows = samples.collect({
      arg sample, iteration;
      /* Create a 'GeneticDrummerRow' */
      GeneticDrummerRow(window, 2, 42+(iteration*42), Window.availableBounds.width-8, 40, background, buttonOn,
        stepOn, buttonStepOn, iteration, numberOfSteps, stepDuration, sample, killSamples);
    });

    /* Invoke 'provideGeneticDrummerRowPeers' */
    this.provideGeneticDrummerRowPeers(geneticDrummerRows);
    /* Invoke 'makecontrollerViewTitles'  */
    this.makecontrollerViewTitles(window, geneticDrummerRows[0].controllerView.bounds);
    /* Invoke 'updateApplicationWindowBounds' */
    this.updateApplicationWindowBounds(window, geneticDrummerRows);

    case
    /* CASE: Metronome has been requested */
    {metronome == true}{
      Task({
        /* Play a Metronome on the default TempoClock */
        {Metro.ar(TempoClock.default.tempo * 60, 1)}.play
      }).play(quant:0);
    }
  }

  /*
  * Take an array of hex Strings and make them colours
  *
  * @param arrayOfColours: an array of hex String
  *
  * @return: An array of 'Color' objects
  */
  setColours {
    arg arrayOfColours;

    ^arrayOfColours.collect({
      arg hexString;
      /* Make a new colour from a hex string */
      Color.fromHexString(hexString);
    })
  }

  /*
  * Calculate the center position for a window based on that windows width and height
  *
  * @param windowWidth: The width of the window being centered
  * @param windowHeight: The height of the window being centered
  *
  * @return: An array a left and a top boundry
  */
  screenCenterPosition {
    arg windowWidth, windowHeight;

    ^[ (Window.screenBounds.right - windowWidth) / 2,
      (Window.screenBounds.bottom - windowHeight) / 2 ];
  }

  /*
  * Create a window for hold a 'GeneticDrummer'
  *
  * @param width: The width of the window
  * @param height: The height of the window
  *
  * @return: A Window
  */
  createApplicationWindow {
    arg width, height; var bounds;

    /* Grab the bounds for a centered window */
    bounds = this.screenCenterPosition(width,height);
    /* Create a 'Window' object; set its background, its visibility and its onClose function; front it */
    ^Window("The Genetic Drummer", Rect(bounds[0], bounds[1], width, height))
    .visible_(true)
    .onClose_({
      "Synth was closed.".postln;
      Server.default.freeAll;
    })
    .front();
  }

  /*
  * Trun a directory of samples into an array of buffers
  *
  * @param directory: A directory containing some samples
  *
  * @return: an Array of Buffers
  */
  setSamples {
    arg directory;

    ^directory.pathMatch.collect{
      arg path;
      Buffer.read(Server.default, path);
    };
  }

  /*
  * Update the bounds of 'GeneticDrummer' so that it fits nice and snuggly after all 'GeneticDrummerRows' have been
  created
  *
  * @param windowToUpdate: The window to have its bounds updated
  * @param allDrummerRows: All the 'GeneticDrummerRows' that where created
  *
  */
  updateApplicationWindowBounds {
    /* Update the GUI to fit */
    arg windowToUpdate, allDrummerRows; var newWidth, newHeight, newLeft, newTop;

    /* Set 'newWidth' to be the width of a 'geneticDrummerRow' with some border */
    newWidth = allDrummerRows[0].geneticDrummerRow.bounds.width + 4;
    /* Set 'newHeight' to be the height of all the 'geneticDrummerRow's created plus the title  */
    newHeight = (allDrummerRows.size+1) * 42;
    /* Set newLeft and newTop to be where the window will be centered */
    # newLeft, newTop = this.screenCenterPosition(newWidth,newHeight);
    /* Update the bounds of the given window */
    windowToUpdate.bounds = Rect(newLeft, newTop, newWidth, newHeight);
  }

  /*
  * Tells every 'GeneticDrummerRows' about its peers i.e. how many other 'GeneticDrummerRows' were created
  *
  * @param geneticDrummerRows: All the 'GeneticDrummerRows' that where created
  *
  */
  provideGeneticDrummerRowPeers {
    arg geneticDrummerRows;
    /* For each 'GeneticDrummerRow'... */
    geneticDrummerRows.do({
      arg aGeneticDrummerRow, iteration;

      case
      /* CASE: this is the first 'GeneticDrummerRow'... */
      {iteration == 0} {
        /* Set 'peers' to be all those 'GeneticDrummerRow's from 1 onwards */
        aGeneticDrummerRow.peers = geneticDrummerRows[1..]
      } /* DEFAULT: this is not the first 'GeneticDrummerRow'... */ {
        /* Set 'peers' to be all those up to the current 'GeneticDrummerRow's and all those from the current
        'GeneticDrummerRow onwards */
        aGeneticDrummerRow.peers = geneticDrummerRows[..(iteration-1)] ++ geneticDrummerRows[(iteration+1)..]
      };
      /* Add a 'nil' to 'peers'; this allows for all peers to be at their correct index */
      aGeneticDrummerRow.peers = aGeneticDrummerRow.peers.insert(iteration, nil);
    });
  }

  /*
  * Add the controller titles to the application window
  *
  * @param parentWindow: The applications window
  * @param controllerViewBounds: The size that this title bar should be
  *
  */
  makecontrollerViewTitles {
    arg parentWindow, controllerViewBounds; var controllerViewControls, titleHolder;

    /* Create a composite view for holding the controller titles */
    controllerViewControls = CompositeView(parentWindow,Rect(controllerViewBounds.left+2, 2,
      controllerViewBounds.width, 40));

    /* Make an initial title to start at the far left of 'controllerViewControls' */
    titleHolder = StaticText(controllerViewControls, Rect(2,0,30,40))
    .string_("Move left")
    .font_(Font("Courier", 10))
    .align_(\center);

    /* Make all the remaining titles! */
    [
      ["Move right", 30],
      ["Populate chance: 0-1", 50],
      ["Populate row", 60],
      ["Crossover Operator", 100],
      ["Parents to mate: x,x", 65],
      ["Mate parents", 50],
      ["Mutation Rate", 50],
      ["Mutate individual", 65],
      ["Kill individual", 65]
    ].do({
      arg titleAndWidth; var title, width;

      # title, width = titleAndWidth;

      /* Update 'titleHolder' to be the last made title */
      /* N.B. This allows for every new title to allign itself to the title left of itself */
      titleHolder = StaticText(controllerViewControls, Rect(titleHolder.bounds.left +
        titleHolder.bounds.width+2,0,width,40))
      .string_(title)
      .font_(Font("Courier", 10))
      .align_(\center);
    });
  }
}