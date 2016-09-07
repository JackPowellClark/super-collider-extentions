/*
* GeneticDrummerRow.sc
* Purpose: A row for The Genetic Drummer
*
* @author: Jack Clark
* @version: 1.0
* @info: Built in SuperCollider 3.6
* @line length limit: 120
*/

GeneticDrummerRow {
  /* Global Variables; variables that are required to be accessible in every scope */
  var <>geneticDrummerRow, rowStepsView, stepRowButtons, currentStep, <>stepSequence, <>stepRowSynth, server,
  <>controllerView, populateChance, mutationRate, parents, <>peers, instanceRowNumber;

  /*
  * Create a new instance of 'GeneticDrummerRow'
  *
  * @param parentWindow: The parent window for this object
  * @param left, top, width, height: Boundaries for 'GeneticDrummerRow'
  * @param background, buttonOn, stepOn, buttonStepOn: Colours for the various button states of 'GeneticDrummerRow'
  * @param rowNumber: The row number of this current 'GeneticDrummerRow'
  * @param numberOfSteps: How many steps 'GeneticDrummerRow' has
  * @param stepSize: The time between the steps of 'GeneticDrummerRow';
  * @param stepRowSample: The sample that this 'GeneticDrummerRow' contains;
  * @param killSamples: The samples for when the individual of 'GeneticDrummerRow' is 'killed';
  *
  * @return: A GeneticRowDrummer
  */
  *new {
    /* Arguments passed into 'GeneticDrummerRow' upon creation */
    arg parentWindow, left, top, width, height, background, buttonOn, stepOn, buttonStepOn, rowNumber,
    numberOfSteps, stepSize, stepRowSample, killSamples;

    /* Invoke 'new' on the superclass; return a new object; invoke the instance method 'initialse' on returned
    object */
    ^super.new.initialse(parentWindow, left, top, width, height, background, buttonOn, stepOn, buttonStepOn,
      rowNumber, numberOfSteps, stepSize, stepRowSample, killSamples)
  }

  /*
  * Initialse 'GeneticDrummerRow', seting the appropriate conditions;.
  *
  * @params: See above
  */
  initialse {
    /* Arguments passed into 'GeneticDrummerRow' */
    arg parentWindow, left, top, width, height, background, buttonOn, stepOn, buttonStepOn, rowNumber,
    numberOfSteps, stepSize, stepRowSample, killSamples;

    /* Get the default server i.e. the local server; set it */
    server = Server.default;
    /* Set the global variable 'instanceRowNumber' */
    instanceRowNumber = rowNumber;
    /* Set the default 'stepSequence' for this 'GeneticDrummerRow' as empty */
    stepSequence = Array.fill(numberOfSteps, {0});
    /* Create an instance of synth 'StepRowPlayBuf' with the given sample as its 'bufNum' */
    stepRowSynth = Synth(\StepRowPlayBuf, [\bufNum, stepRowSample]);

    /* Create a composite view for holding this 'GeneticDrummerRow'; set its background */
    geneticDrummerRow = CompositeView(parentWindow, Rect(left,top,width,height))
    .background_(background);

    /* Make the specified number of steps with the given colours; N.B.the specified width of '500' defines the size
    of the step sequencer */
    this.makeRowSteps(geneticDrummerRow, 500, height, background, buttonOn, stepOn, buttonStepOn, numberOfSteps);
    /* Start 'StepRowTask' */
    this.makeStepRowTask(numberOfSteps, stepSize).play;
    /* Make controls for controling this row's steps */
    this.makeGeneticRowControls(geneticDrummerRow, rowStepsView.bounds.width+4,
      geneticDrummerRow.bounds.width-rowStepsView.bounds.width-6, height, numberOfSteps, stepRowSample,
      killSamples);
    /* Update the width of 'controllerView' to fit its children */
    geneticDrummerRow.maxWidth_(controllerView.bounds.left + controllerView.bounds.width+2);
  }

  /*
  * Create the steps for 'GeneticDrummerRow'
  *
  * @param parentWindow: The parent window for this object
  * @param width, height: Boundaries for 'makeRowSteps'
  * @param background, buttonOn, stepOn, buttonStepOn: Colours for the various button states of 'GeneticDrummerRow'
  * @param numberOfSteps: How many steps 'GeneticDrummerRow' has
  *
  * @return: A GeneticRowDrummer
  */
  makeRowSteps {
    arg parentWindow, width, height, background, buttonOn, stepOn, buttonStepOn, numberOfSteps;
    var newWidth, newHeight, buttonWidth, buttonHeight, rowNumberStaticText;

    /* Calculate a new width for 'rowStepsView' by giving a little border */
    newWidth = width-2;
    /* Calculate a new height for 'rowStepsView' by giving a little border */
    newHeight = height-4;
    /* Calculate the size for a buttons width with a little border */
    buttonWidth = ((newWidth-4)/numberOfSteps);
    /* Give the button height a little border */
    buttonHeight = (newHeight-4);

    /* Create a composite view for holding this 'GeneticDrummerRow'; set its background */
    rowStepsView = CompositeView(parentWindow, Rect(2, 2, newWidth+22, newHeight));
    /* Make static text box; set it to show the row number */
    rowNumberStaticText = StaticText(rowStepsView, Rect(2,4,20,buttonHeight))
    .string_(instanceRowNumber)
    .align_(\center);

    /* For the specified number of steps, create a step */
    stepRowButtons = Array.fill(numberOfSteps, {
      arg step; var button;

      /* Create a button; Set the colours for its four states*/
      button = Button(rowStepsView, Rect(rowNumberStaticText.bounds.left + rowNumberStaticText.bounds.width + 2 +
        (step*buttonWidth), 2, (buttonWidth), buttonHeight))
      .states_([
        ["",Color.black, background],
        ["",Color.black, buttonOn],
        ["",Color.black, stepOn],
        ["",Color.black, buttonStepOn]
      ]);

      /* Set the action function evaluated whene the button is pressed */
      button.action_({
        case
        /* CASE: the step is 0... */
        {stepSequence[step] == 0} {
          /* Set the step to 1 */
          stepSequence[step] = 1;
          /* Update the buttons value (its state) */
          button.value_(1);
        } /* CASE: the step is 1... */ {
          /* Set the step to 0 */
          stepSequence[step] = 0;
          /* Update the buttons value (its state) */
          button.value_(0);
        };
      });
    });
  }

  /*
  * Create the controlls for 'GeneticDrummerRow'
  *
  * @param parentWindow: The parent window for this object
  * @param left, width, height: Boundaries for 'makeRowSteps'
  * @param numberOfSteps: How many steps 'GeneticDrummerRow' has
  * @param stepRowSample: The sample that this 'GeneticDrummerRow' contains
  * @param killSamples: The samples for when the individual of 'GeneticDrummerRow' is 'killed'
  */
  makeGeneticRowControls {
    arg parentWindow, left, width, height, numberOfSteps, stepRowSample, killSamples;

    var newWidth, newHeight, buttonHeight, moveIndividualLeft, moveIndividualRight, populateChanceStaticField,
    populateChanceTextField, populateButton, matingMenu, matingTextField, matingButton, mutationRateStaticField,
    mutationRateTextField, mutateButton, killButton;

    /* Calculate a new width for 'rowStepsView'; everyone likes a little it of border */
    newWidth = width-4;
    /* Calculate a new height for 'rowStepsView'; everyone likes a little it of border */
    newHeight = height-4;
    /* Give the button height a small border */
    buttonHeight = (newHeight-4);
    /* Create a composite view for holding the controllerView for 'GeneticDrummerRow' */
    controllerView = CompositeView(parentWindow, Rect(left, 2, width, newHeight));

    /* Create a button for moving a individual one step to the left */
    moveIndividualLeft = Button(controllerView, Rect(2,2,30, buttonHeight))
    .states_([["<--"]])
    .action_({
      /* Invoke 'moveIndividual' */
      this.moveIndividual(-1);
    });

    /* Create a button for moving a individual one step to the left */
    moveIndividualRight = Button(controllerView, Rect(moveIndividualLeft.bounds.left +
      moveIndividualLeft.bounds.width+2,2,30, buttonHeight))
    .states_([["-->"]])
    .action_({
      /* Invoke 'moveIndividual' */
      this.moveIndividual(1);
    });

    /* Create a text field to allow for a user input of the populate chance */
    populateChanceTextField = TextField(controllerView, Rect(moveIndividualRight.bounds.left +
      moveIndividualRight.bounds.width+2, 2, 50, buttonHeight))
    .string_("")
    .action_({
      arg field;
      /* Update 'populateChance' to equal the value held in this text field */
      populateChance = field.value.asFloat;
    });

    /* Create a button for populating a row's steps */
    populateButton = Button(controllerView, Rect(populateChanceTextField.bounds.left +
      populateChanceTextField.bounds.width+2,2,60,buttonHeight))
    .states_([["Populate"]])
    .action_({
      /* Force the action of 'populateChanceTextField', updatinf the global variable 'populateChance' to be the
      string held in 'populateChanceTextField' */
      populateChanceTextField.doAction;

      case
      /* CASE: 'populateChance' is between 0 and 1 */
      {(populateChance <= 1) && (populateChance > 0)} {
        /* Set 'stepSequence' to be the result of invoking 'beholdANewIndividual' */
        stepSequence = this.beholdANewIndividual(numberOfSteps);
        /* Invoke 'updateRow' */
        this.updateRow(numberOfSteps);
      } /* DEFAULT: 'populateChance' was not between 0 and 1 */ {
        /* Invoke 'throwError' with a specified message */
        this.throwError("Please enter a float value between 0 and 1");
      }
    });

    /* Create a pop up menu for holding all the crossover operators */
    matingMenu = PopUpMenu(controllerView,Rect(populateButton.bounds.left +
      populateButton.bounds.width+2,2,100,buttonHeight))
    .items_(["Random","Single Point","Two Point"])
    .font_(Font("Courier", 10));

    /* Create a text field to allow for a user input the parents for mating */
    matingTextField = TextField(controllerView, Rect(matingMenu.bounds.left + matingMenu.bounds.width+2, 2, 65,
      buttonHeight))
    .string_("")
    .action_({
      arg field;
      /* Update 'parents' to equal the value held in this text field */
      parents = field.value;
    });

    /* Create a button for mating two rows */
    matingButton = Button(controllerView, Rect(matingTextField.bounds.left +
      matingTextField.bounds.width+2,2,50,buttonHeight))
    .states_([["Mate"]])
    .action_({
      /* Force the action of 'matingTextField', updating the global variable 'parents' to be the string held in
      'matingTextField' */
      matingTextField.doAction;

      /* Input checking */
      case
      /* CASE: the string in 'parents' does... contain a comma AND have a size of 2 when split on commas AND not
      contain a comma at it's end or start */
      {(parents.contains(",")) && (parents.split($,).size == 2) && ((parents.find(",") != (parents.size-1)) &&
        (parents.find(",") != 0))} {
        var firstParent, secondParent;
        /* Split the 'parents' on its comma */
        # firstParent, secondParent = parents.split($,).asInteger;
        /* Invoke 'itMustBeMatingSeason' */
        this.itMustBeMatingSeason(firstParent, secondParent, matingMenu.value, numberOfSteps);
        /* Invoke 'updateRow' */
        this.updateRow(numberOfSteps);
      } /* DEFAULT: criteria not matched... */ {
        /* Invoke 'throwError' with a specified message */
        this.throwError("Please ensure that the value given for parents is of form 'x,x'");
      }
    });

    /* Create a text field to allow for a user input the mutation rate */
    mutationRateTextField = TextField(controllerView, Rect(matingButton.bounds.left +
      matingButton.bounds.width+2, 2, 50, buttonHeight))
    .string_("")
    .action_({
      arg field;
      /* Update 'mutationRate' to equal the value held in this text field */
      mutationRate = field.value.asFloat;
    });

    /* Create a button for mutating a row */
    mutateButton = Button(controllerView, Rect(mutationRateTextField.bounds.left +
      mutationRateTextField.bounds.width+2,2,65,buttonHeight))
    .states_([["Mutate"]])
    .action_({
      /* Force the action of 'mutationRateTextField', updating the global variable 'mutationRate' to be the string
      held in 'mutationRateTextField' */
      mutationRateTextField.doAction;
      /* Set 'stepSequence' to be the result of invoking 'mutateIndividual' */
      stepSequence = this.mutateIndividual(stepSequence);
      /* Invoke 'updateRow' */
      this.updateRow(numberOfSteps);
    });

    /* Create a button for 'killing' a individual */
    killButton = Button(controllerView, Rect(mutateButton.bounds.left +
      mutateButton.bounds.width+2,2,65,buttonHeight))
    .states_([["Kill"]])
    .action_({
      var tempSynth;
      /* Set 'stepSequence' to empty */
      stepSequence = Array.fill(numberOfSteps, {0});
      /* Invoke 'updateRow' */
      this.updateRow(stepSequence.size);
      /* Play a random 'kill' sound */
      tempSynth = Synth(\killSoundsPlayBuf, [\bufNum, killSamples[killSamples.size.rand]])
    });

    /* Update the width of 'controllerView' to fit its children */
    controllerView.maxWidth_(killButton.bounds.left + killButton.bounds.width+2);
  }

  /*
  * Create a task for performing the default step sequencer functionality
  *
  * @param numberOfSteps: How many steps 'GeneticDrummerRow' has
  * @param stepSize: The time between the steps of 'GeneticDrummerRow'
  *
  * @return: A Task
  */
  makeStepRowTask {
    arg numberOfSteps, stepSize;

    ^Task({ loop {
      /* For ever step... */
      numberOfSteps.do({
        arg step; {
          /* Update the global variable 'currentStep' */
          currentStep = step;

          case
          /* CASE: 'stepSequence' indicates the current step should be 'ON' */
          {stepSequence[step] == 1} {
            /* Set the current button to 'buttonStepOn' */
            stepRowButtons[step].value_(3);
            /* Trigger the synth associated with this row */
            stepRowSynth.set(\trig, 1);
          } /* DEFAULT: the current step should be 'OFF' */ {
            /* Set the current button to 'stepOn' */
            stepRowButtons[step].value_(2);
          }
          /* Setting GUI values is asynchronous; the evaluation of this function must be defered */
        }.defer;
        /* A small weight to guarantee 'trig' resets */
        0.05.wait;
        /* Update the trigger of the synth associated with this row */
        stepRowSynth.set(\trig, -1);
        /* Wait for the given stepSize minus the above wait */
        (stepSize-0.05).wait;

        /* Having performed all functionality for a step, reset it */
        {
          case
          /* CASE: 'stepSequence' indicates the current step should be 'OFF' */
          {stepSequence[step] == 0} {
            /* Set the current button to 'buttonOff' */
            stepRowButtons[step].value_(0);
          } /* DEFAULT: the current step should be 'OFF' */ {
            /* Set the current button to 'buttonOn' */
            stepRowButtons[step].value_(1);
          }
          /* Setting GUI values is asynchronous; the evaluation of this function must be defered */
        }.defer;
      })
    }})
  }

  /*
  * Update the GUI if a 'GeneticDrummerRow'
  *
  * @param numberOfSteps: How many steps 'GeneticDrummerRow' has
  */
  updateRow {
    arg numberOfSteps;

    /* For ever step... */
    numberOfSteps.do({
      arg step;
      {
        case
        /* CASE: the current step we are on, IS NOT the one step is on */
        {step != currentStep} {
          /* CASE: 'stepSequence' indicates the current step should be 'ON' */
          case
          {stepSequence[step] == 1} {
            // Set to 'state' 1 i.e. button on.
            stepRowButtons[step].value_(1);
          } /* DEFUALT: the current column should be 'OFF' */{
            // Set to 'state' 0 i.e. button off.
            stepRowButtons[step].value_(0);
          }
        }
        /* Setting GUI values is asynchronous; the evaluation of this function must be defered */
      }.defer
    });
  }

  /*
  * Populate a row
  *
  * @param numberOfSteps: How many steps 'GeneticDrummerRow' has
  *
  * @return: A new 'stepSequece'/the current 'stepSequence'
  */
  beholdANewIndividual {
    arg numberOfSteps; var left, top, errorWindow;

    case
    /* CASE: 'populateChance' was between 0 and 1 */
    {(populateChance <= 1) && (populateChance > 0)} {
      /* Return an Array where each item is the resut of a random test whose probability of success
      'populateChance' */
      ^Array.fill(numberOfSteps, {populateChance.coin.binaryValue});
    } {
      /* Invoke 'throwError' with a specified message */
      this.throwError("Please enter a float value between 0 and 1");
      /* Return 'stepSequence' as is */
      ^stepSequence;
    };
  }

  /*
  * Mutate the current row
  *
  * @param individual: a rows current individual ('stepSequece')
  *
  * @return: A mutated version of 'stepSequece'
  */
  mutateIndividual {
    arg individual;

    /* Return a new individual */
    ^individual.collect({
      arg gene; var newgene = gene;

      case
      /* CASE: 'gene' is equal to 1 and triggered a mutation */
      {gene == 1 && mutationRate.coin == true} {
        /* Set 'newgene' to 0 */
        newgene = 0;
      } /* CASE: 'gene' is equal to 0 and triggered a mutation  */
      {gene == 0 && mutationRate.coin == true} {
        /* Set 'newgene' to 1 */
        newgene = 1;
      };

      /* Return 'newgene' to 'collect' regardless of it is was mutated */
      newgene;
    });
  }

  /*
  * Move a individual
  *
  * @param direction: The direction to move a individual
  */
  moveIndividual {
    arg direction;

    /* Set 'stepSequence' to be a rotated version of itself */
    stepSequence = stepSequence.rotate(direction);
    /* Invoke 'updateRow' */
    this.updateRow(stepSequence.size);
  }

  /*
  * Mate two rows to create an offspring
  *
  * @param firstParent: An offsprings first parent
  * @param secondParent: An offsprings second parent
  * @param matingType: The crossover operator to use
  * @param numberOfSteps: How many steps 'GeneticDrummerRow' has
  */
  itMustBeMatingSeason {
    arg firstParent, secondParent, matingType, numberOfSteps;

    /* Although the given argument was of the correct form... */
    case
    /* CASE: both 'firstParent' and 'secondParent' are rows that actually exist... */
    {(firstParent <= numberOfSteps) && (firstParent != instanceRowNumber) && (secondParent <= numberOfSteps) &&
      (secondParent != instanceRowNumber)}{
      /* Helpful print statements */
      ("Mating rows: " ++ firstParent ++ " and " ++ secondParent ++ ". Operator: ").post;
      ("Random crossover").postln;

      /* Apply a crossover operator */
      case
      /* CASE: the requested crossover operator is 'Random' */
      {matingType == 0}{
        stepSequence = numberOfSteps.collect({
          arg step;

          case
          /* CASE: the coin was tails */
          {0.5.coin}{
            /* Return the gene held in 'firstParent' for the current step to 'collect' */
            peers[firstParent].stepSequence[step];
          }
          /* DEFAULT: the coin was heads */{
            /* Return the gene held in 'secondParent' for the current step to 'collect' */
            peers[secondParent].stepSequence[step];
          }
        });
      }
      /* CASE: the requested crossover operator is 'Single Point Crossover' */
      {matingType == 1}{
        var randomPositionK, parentOneGenes, parentTwoGenes;

        /* Set a random cut position K */
        randomPositionK = numberOfSteps.rand;
        /* Take all of 'firstParent' genes up to the first cut */
        parentOneGenes = peers[firstParent].stepSequence[..randomPositionK];
        /* Take all of 'secondParent' genes from the first cut up to the end */
        parentTwoGenes = peers[secondParent].stepSequence[randomPositionK+1..];
        /* Set 'stepSequence' to be the concatenation 'parentOne1' and 'parentTwo1' */
        stepSequence = parentOneGenes ++ parentTwoGenes;

        /* Helpful print statements */
        ("Single point crossover").postln;
        ("The random cut-point is: " ++ randomPositionK).postln;
        ("Parent one gentic information: " ++ parentOneGenes).postln;
        ("Parent two gentic information: " ++ parentTwoGenes).postln;
      }
      /* CASE: the requested crossover operator is 'Two Point Crossover' */
      {matingType == 2}{
        var randomPositionK1, randomPositionK2, parentOne1, parentOne2, parentTwo1;

        /* Set a random cut position K1 */
        randomPositionK1 = numberOfSteps.rand;
        /* Set a random cut position K2 */
        randomPositionK2 = numberOfSteps.rand;


        case
        /* CASE: 'randomPositionK1' is greater than 'randomPositionK2' */
        {randomPositionK1 > randomPositionK2} {
          var placeHolder;

          /* Set 'randomPositionK1' to always be the smallest of the two random positions */
          placeHolder = randomPositionK1;
          randomPositionK1 = randomPositionK2;
          randomPositionK2 = placeHolder;
        };

        /* Take all of 'firstParent' genes up to the first cut */
        parentOne1 = peers[firstParent].stepSequence[..randomPositionK1];
        /* Take all of 'secondParent' genes from the first cut up to the second cut */
        parentTwo1 = peers[secondParent].stepSequence[randomPositionK1+1..randomPositionK2];
        /* Take all of 'firstParent' genes from the the second cut up to the end */
        parentOne2 = peers[firstParent].stepSequence[randomPositionK2+1..];
        /* Set 'stepSequence' to be the concatenation 'parentOne1', 'parentTwo1' and 'parentOne2' */
        stepSequence = parentOne1 ++ parentTwo1 ++ parentOne2;

        /* Helpful print statements */
        ("Double point crossover").postln;
        ("The random cut-points are: " ++ randomPositionK1 ++ " and " ++ randomPositionK2).postln;
        ("Parent one first cut gentic information: " ++ parentOne1).postln;
        ("Parent two middle section gentic information: " ++ parentTwo1).postln;
        ("Parent one last cut gentic information: " ++ parentOne2).postln;
      }
    } /* DEFAULT: either or both of 'firstParent' and 'secondParent' don't actually exist...  */ {
      /* Invoke 'throwError' with a specified message */
      this.throwError("One (or both!) of the given parents does not exist (or it is this row). Please check the
      row numbers of specified parents.");
    };
  }

  /*
  * Throw an error window
  *
  * @param errorMessage: The error message to display
  */
  throwError {
    arg errorMessage; var left, top, errorWindow;

    /* Get the left postion to center the error */
    left = (Window.screenBounds.right - 212) / 2;
    /* Get the top postion to center the error */
    top = (Window.screenBounds.bottom - 163) / 2;

    /* Create  Window for holding the error */
    errorWindow = Window.new("This is awkward...", Rect(left, top, 212, 163))
    .background_(Color.fromHexString("BDBDBD"));

    /* Add the error to the window as static text */
    StaticText(errorWindow, errorWindow.bounds.moveTo(0,0).insetBy(12))
    .string_(errorMessage)
    .font_(Font("Monaco", 14))
    .align_(\center)
    .background_(Color.clear);

    /* Fron the window*/
    errorWindow.front;
  }
}