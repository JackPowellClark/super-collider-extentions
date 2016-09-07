/*
* SCSonicAnnotator.sc
* Purpose: A wrapper class for Sonic Annotator - a batch tool for feature extraction and annotation of audio files using Vamp
* 		   plugins
*
* @author: Jack Clark
* @version: 1.0
*
* @info: Built in SuperCollider 3.6.6
* @line length limit: 120
*/

SCSonicAnnotator {
  /* Environment Variables: variables that are required to be accessible in every scope */
  classvar pS, transformDir, sonicAnnotatorDir, vampPathSet, vampCollections;

  /*
  * Initialize the class; build/save a transform file for each output
  */
  *installPlugins {
    var server;
    /* Get the default server i.e. the local server */
    server = Server.default;

    /* Run as a 'Routine' N.B. this allows for 'sync' methods */
    Routine.run {
      /* Invoke 'setGlobalDirectories' */
      this.setGlobalDirectories;
      /* Check all pending asynchronous commands have been completed */
      server.sync;
      /* Force a wait; helpful print statement */
      0.5.wait; "\nSetting Plugin Directories.".post; 0.25.wait; ".".post; 0.25.wait; ".".postln;

      /* Retrieve all the known Vamp Transform IDs */
      vampCollections = this.getKnownIDs;
      /* Check  all pending asynchronous commands have been completed */
      server.sync;
      /* Force a wait */
      0.5.wait; "Collecting Known Vamp Transform IDs.".post; 0.25.wait; ".".post; 0.25.wait; ".".postln;

      /* Helpful print statement */
      "Saving Transforms: ".post;
      /* Invoke 'saveTransforms' */
      this.saveTransforms();
      /* Check  all pending asynchronous commands have been completed */
      server.sync;
      /* Post status */
      "Transforms Saved.".postln;

      /* Helpful print statement */
      "\nAll Plugins Installed.\n".postln;
    };
  }

  /*
  * Set all global variables and the environment variable 'VAMP_PATH'; listing the locations a host should look in for
   Vamp plugins
  *
  * @param systemDirectory: Whether to set the environment variable 'VAMP_PATH' to a system or local directory
  */
  *setGlobalDirectories {
    arg systemDirectory = false; var extensionDir, extensionPluginDir, defaultPluginDir;

    /* Set a variable to hold the platform specific path separator */
    pS = Platform.pathSeparator;
    /* Set full extension directory */
    extensionDir = (Platform.userExtensionDir ++ pS ++ "SuperColliderExtensions" ++ pS ++ "SonicAnnotator");
    /* Set a platform specific extension plugin directory */
    extensionPluginDir = (extensionDir ++ pS ++ "VampPlugins" ++ pS).quote;
    /* Set the directory of all saved transforms */
    transformDir = (extensionDir ++ pS ++ "Transforms" ++ pS);
    /* Set the 'sonic-annotator' directory */
    sonicAnnotatorDir = (extensionDir ++ pS ++ "SonicAnnotator" ++ pS).quote;

    /* Locate system or local plugin directory */
    case{systemDirectory} {
      defaultPluginDir = "/Library/Audio/Plug-Ins/Vamp".quote
    } {
      defaultPluginDir = "$HOME/Library/Audio/Plug-Ins/Vamp".quote
    };

    /* Set 'vampPathSet' as unix command that will export a variable to the environment of all the child processes
    running in the current shell.*/
    vampPathSet = ("export VAMP_PATH=" ++ defaultPluginDir ++ ":" ++ extensionPluginDir ++ " ; ");
  }

  /*
  * Build a MultiLevelIdentityDictionary of 'known' transform IDs i.e. all the Vamp plugins installed
  *
  * @return: An Identity Dictionary of known transform IDs
  */
  *getKnownIDs {
    var knownTransformIDs, vampCollections;

    /* Invoke 'setGlobalDirectories' */
    this.setGlobalDirectories;

    /* Create an empty list; fill it with all known transform IDs */
    knownTransformIDs = List();
    /* For all installed Vamp plugins and their available outputs... */
    (vampPathSet ++ sonicAnnotatorDir ++ "sonic-annotator -l").unixCmdGetStdOutLines.do({
      arg availableOutput;

      /* Strip away the starting 5 characters */
      knownTransformIDs.add(availableOutput[5..availableOutput.size].toLower);
    });

    /* Sort 'knownTransformIDs' so there is no unexpected issues when building 'collections' */
    knownTransformIDs = knownTransformIDs.sort;

    /* Create a MultiLevelDictionary to hold all vamp collections */
    vampCollections = MultiLevelIdentityDictionary();
    /* For all of the knownTransformIDs... */
    knownTransformIDs.do({
      arg transformID; var collection, plugin, output;

      /* Split a 'transformID' on each ':' to extract its three parts */
      # collection, plugin, output = transformID.split($:);

      /* Make 'collection' and 'plugin' Symbols */
      # collection, plugin = [collection.asSymbol, plugin.asSymbol];

      /* Build the dictionary 'vampCollections' */
      case
      /*  CASE - there is no collection in 'vampCollections' for 'collection'... */
      {vampCollections.at[collection].isNil} {
        /* Make it */
        vampCollections.put(collection, plugin, List[output]);
      }
      /* CASE - there is no plugin in 'vampCollections' for 'collection'... */
      {vampCollections.at[collection][plugin].isNil} {
        /* Make it */
        vampCollections.put(collection, plugin, List[output]);
      } /* DEFAULT - there must already be a collection and plugin for 'collection' and 'plugin' in
      'vampCollections'... */ {
        /* Add the new output to them */
        vampCollections.at[collection][plugin].add(output);
      }
    });

    /* Convert the current MultiLevelIdentityDictionary into an IdentityDictionary; return it */
    ^vampCollections.dictionary;
  }

  /*
  * Saves transforms for all known transform IDs into structured folders
  */
  *saveTransforms {
    var server, saveDir;

    /* Get the default server i.e. the local server */
    server = Server.default;
    /* Set the 'saveDir' i.e. where transforms will save to */
    saveDir = (Platform.userExtensionDir ++ pS ++ "SuperColliderExtensions" ++ pS ++ "SonicAnnotator" ++ pS).quote;
    /* Change into this directory; make a new directory: 'Transforms' */
    ("cd " ++ saveDir ++ " ; mkdir -p " ++ "Transforms" ++ pS).unixCmdGetStdOut;
    /* Update the saveDir to the newly made directory */
    saveDir = saveDir ++ "Transforms" ++ pS;

    /* Using 'vampCollections' to name directories and transforms: */
    /* For all key and value pairs in 'VampCollections'... */
    vampCollections.keysValuesDo({
      arg collection, plugins; var collectionDir;

      /* Set the collection directory, a 'parsed' collection name */
      collectionDir = collection.asString.split($-)[0].quote ++ "/";

      /* For all the key and value pairs in 'plugins'... */
      plugins.keysValuesDo({
        arg plugin, outputList; var pluginDir;

        /* CASE - the plugin name includes a '-'... */
        case {plugin.asString.findBackwards("-").notNil} {
          /* Parse it; set the plugin directory */
          pluginDir = plugin.asString[plugin.asString.find("-")+1.. plugin.asString.size].quote  ++ "/";
        } /* DEFAULT - the pluginKey is already formatted acceptably ... */ {
          /* Set the plugin directory */
          pluginDir = plugin.asString.quote ++ "/";
        };

        /* Change directory into the save directory; make the necessary plugin directory */
        ("cd " ++ saveDir ++ " ; mkdir -p " ++ collectionDir ++ pluginDir).unixCmdGetStdOut;

        /* For all the outputs in 'outputList'... */
        outputList.do({
          arg output;

          /* Run the following as a 'Routine' allowing for 'sync' methods */
          Routine.run {
            /* Change into an outputs parent directory; save a transform file for the output */
            (vampPathSet ++ "cd "++ saveDir ++ collectionDir ++ pluginDir ++ " ; " ++ sonicAnnotatorDir ++
              "sonic-annotator -s vamp:" ++ collection ++ ":" ++ plugin ++ ":" ++ output ++ " > " ++
              output ++ ".n3").unixCmdGetStdOut;
            /* Check  all pending asynchronous commands have been completed */
            server.sync;
          };
        });
      });
    });
  }

  /*
  * Post all the available outputs i.e. features that can be extracted
  */
  *availableOutputs {
    /* Invoke 'setGlobalDirectories' */
    this.setGlobalDirectories;

    case
    /*  CASE - A transform folder exists... */
    {File.exists(transformDir)} {
      /* Retrieve all the known Vamp Transform IDs */
      vampCollections = this.getKnownIDs;

      /* For all the collections in 'vampCollections'... */
      vampCollections.keysValuesDo({
        arg collection, plugins;

        /* Set the collection key as a 'parsed' collection name */
        collection = collection.asString.split($-)[0];

        /* Post the collection key */
        "".postln; (collection).postln;

        /* For all the plugins dictionaries... */
        plugins.keysValuesDo({
          arg plugin, outputList;

          /* CASE - the plugin name includes a '-'... */
          case {plugin.asString.findBackwards("-").notNil} {
            /* parse it; set the plugin directory */
            plugin = plugin.asString[plugin.asString.find("-")+1.. plugin.asString.size];
          } /* DEFAULT - the pluginKey is already formatted acceptably ... */ {
            /* Set the plugin key as a String */
            plugin = plugin.asString;
          };

          /* Post the plugin key; padded for a tabs width */
          (plugin.padLeft(plugin.size + 4)).postln;

          /* For all the outputs in 'outputList'... */
          outputList.do({
            arg output;

            /* Set the output as a String */
            output = output.asString;
            /* Post the output; padded for two tabs width */
            (output.padLeft(output.size + 8)).postln;
          });
        });
      });
    } /*  DEFAULT - A transform folder does not exists... */ {
      "Could not locate a Transform Directory at: ".post; transformDir.postln;
      "\nHave you ran SonicAnnotator.installPlugins?".postln;
    }
  }

  /*
  * Extract the requested features for a given audio file
  *
  * @param audioFilePath: The path of an audio file
  * @param featureArray: A nested array of features to extract
  * @param fullPathReturn: Boolean indicating whether an output's full path is returned
  *
  * @return: An Identity Dictionary of extracted feature results
  */
  *extractFeatures {
    arg audioFilePath, featureArray, fullPathReturn; var tmpDir, scriptName, script, scriptContents, collection,
    plugin, output, transformPath, features;

    /* Invoke 'setPluginDirectories' */
    this.setGlobalDirectories;

    case
    /*  CASE - A transform folder exists... */
    {File.exists(transformDir)} {
      /* Get a user's default temporary directory  */
      tmpDir = (Platform.userExtensionDir ++ pS ++ "SuperColliderExtensions" ++ pS ++ "SonicAnnotator" ++ pS ++
        "tmp" ++ pS);
      /* Make a script name */
      scriptName = "sonicAnnotatorBatchExtract.sh";
      /* Make a tmp file for holding a bash script; open it */
      script = File(Platform.defaultTempDir ++ scriptName, "w");
      /* Set the variable for holding the script contents; start with a directory check */
      scriptContents = "if [ -d " ++ tmpDir.escapeChar($ ) ++ " ]\nthen\nrm -rf " ++ tmpDir.escapeChar($ ) ++
        "\nfi\n" ++ "mkdir " ++ tmpDir.escapeChar($ ) ++ "\n\n";

      /* For the given feature array... */
      featureArray.do({
        arg collection_pluginRequests; var collection, pluginRequests;

        /* Separate the collection from their partnered plugin requests */
        # collection, pluginRequests = collection_pluginRequests;

        /* For the given plugin request... */
        pluginRequests.do({
          arg plugin_outputRequests; var plugin, outputRequests;

          /* Separate the plugin from their partnered output requests */
          # plugin, outputRequests = plugin_outputRequests;

          /* For the given output request... */
          outputRequests.do({
            arg output;

            /* Set the transform path i.e. the path of the requested features transform */
            transformPath = (transformDir ++ collection ++ pS ++ plugin ++ pS ++ output ++ ".n3").quote;

            /* Get the current script contents; add a sonic annotator extract command to it */
            scriptContents = scriptContents ++ (vampPathSet ++ sonicAnnotatorDir ++ "sonic-annotator -t " ++
              transformPath ++ " -w csv --csv-stdout " ++ audioFilePath.quote) ++ " > " ++
            (tmpDir ++ collection ++ "_" ++ plugin ++ "_" ++ output ++ ".txt").quote ++ " &\n";
          });
        });
      });

      /* Get the variable for holding script contents; add a 'wait' onto the end */
      scriptContents = scriptContents + "\nwait";
      /* Write to the tmp file */
      script.write(scriptContents);
      /* Close the file */
      script.close;

      /* Helpful print statement */
      ("\nExtracting requested features for: " ++ PathName(audioFilePath).fileNameWithoutExtension).postln;
      /* Set the permissions for script; run the script */
      ("cd " ++ Platform.defaultTempDir ++ "; chmod +x " ++ scriptName ++ "; ./" ++ scriptName).unixCmdGetStdOut;

      /* Make a MultiLevelIdentityDictionary for holding features */
      features = MultiLevelIdentityDictionary();
      /* For all the newly extracted features... */
      (tmpDir ++ "*.txt").pathMatch.do({
        arg path; var stringPath, collection, plugin, outputStart, output, extractedFeature, quoteFound,
        parsedResults;

        /* Set 'stringPath' */
        stringPath = PathName(path).fileNameWithoutExtension;
        /* Split the path into it's collection, plugin and output; set collection and plugin as the first two
        indexes */
        # collection, plugin = stringPath.split($_);
        /* Find the second '_' in */
        outputStart = stringPath.findAll("_")[1];
        /* Set output from the second '_' onwards */
        output = stringPath[outputStart+1..];
        /* Open the txt file; read it into an Array which is split on new lines */
        extractedFeature = CSVFileReader.read(path, true);
        /* Set 'quoteFound'; N.B. this is in case there are values containing '"' */
        quoteFound = false;

        /* For the first line i.e. the line that contains a file name.. */
        extractedFeature[0].do({
          arg item, firstLineIteration; var toRemove, pathEndIndex;

          /* Check to see if the path includes a ',' */
          case
          /* If an the current item is not the firt index OR the last AND ends with a '"' AND it is the first
          '"' to be found... */
          {(firstLineIteration != 0) && (firstLineIteration != (extractedFeature[0].size - 1)) &&
            (item.endsWith("\"")) && (quoteFound == false)} {
            /* Make a list for holding the indexes that need removing */

            toRemove = List();
            /* Set 'pathEndIndex' i.e. the index where the path ends */
            pathEndIndex = firstLineIteration;

            /* For all the first line indexes that contain parts of 'path'  */
            extractedFeature[0][1..pathEndIndex].size.do({
              arg iteration;

              /* Concatenate the first index with the path part at 'iteration' */
              extractedFeature[0][0] = extractedFeature[0][0] ++ extractedFeature[0][iteration + 1];
              /* Add an index for removal to 'toRemove' */
              toRemove.add(iteration + 1);
            });

            /* For the indexes needing removal... */
            toRemove.size.do({
              /* Remove the index at '1' */
              extractedFeature[0].removeAt(1);
            });

            /* Set quote found as 'true'; N.B. incase there are values containing '"' */
            quoteFound = true;
          };
        });

        /* Make a List for holding parsed results */
        parsedResults = List();
        /* For each raw extracted feature... */
        extractedFeature.do({
          arg item;

          /* Drop the first index; round the result to two decimal places; add to 'parsedResults' */
          parsedResults.add(item.asFloat.drop(1));
        });

        /* Make 'parsedResults' an Array */
        parsedResults = parsedResults.asArray;

        /* Perform 'cleaning' on parsedResults */
        case
        /* CASE - The extracted feature is of form +,1... */
        {(parsedResults.size > 1) && (parsedResults.first.size == 1)} {
          "One of the requested features is of form [time,time,time...]. This will be a feature which
          indicates when a certain event occurs e.g. an onset.\nThese can not be concatenated in any manner
          and will thus be returned as is.".postln;
        }
        /* CASE - The extracted feature is of form 1,2... */
        {(parsedResults.size == 1) && (parsedResults.first.size == 2)} {
          /* Strip away time */
          parsedResults = parsedResults.first.drop(1);
        }
        /* CASE - The extracted feature is of form +,+... */
        {(parsedResults.size > 1) && (parsedResults.first.size > 1)} {
          /* Strip away time */
          parsedResults.do({
            arg result, iteration;

            parsedResults[iteration] = result.drop(1);
          });

          parsedResults = parsedResults.mean;
        }
        /* CASE - The extracted feature is of form 1,2+... */
        {(parsedResults.size == 1) && (parsedResults.first.size > 2)} {
          /* Strip away time */
          parsedResults = parsedResults.first.drop(1);
        } /* DEFAULT - None of the above cases were triggered... */ {
          "Warning: A value of a previously unseen format was returned.\nThere may be some illegable results
          in the returned Dictionary/csv file.\n".postln;
        };

        /* Finally, round all results to two decimal places */
        parsedResults = parsedResults.asFloat.round(0.01);

        /* Put 'parsedresults' into 'features' */
        case
        /* CASE - put with full output path... */
        {fullPathReturn} {
          features.put(collection.asSymbol, plugin.asSymbol, output.asSymbol, parsedResults);
        } /* CASE - put with only output name... */ {
          features.put((output ++ " (" ++ collection ++ ")").asSymbol, parsedResults);
        };
      });

      /* All features retrieved; delete 'tmpDir' */
      ("rm -rf " ++ (tmpDir.quote)).unixCmd;

      /* Convert the current features MultiLevelIdentityDictionary into an IdentityDictionary; return it */
      ^features.dictionary;
    } /*  DEFAULT - A transform folder does not exists... */ {
      "Could not locate a Transform Directory at: ".post; transformDir.postln;
      "\nHave you ran SonicAnnotator.installPlugins?".postln;
    }
  }

  /*
  * Extract the requested features for a given array of audio file; write the results to csv
  *
  * @param audioFilePathArray: An array of audio paths
  * @param featureArray: A nested array of features to extract
  * @param csvWriteDirectory: The directory in which to write results to
  *
  * @return: The content generated to write a csv file; N.B. this is only returned if 'csvWriteDirectory' is nil
  */
  *batchExtractFeatures {
    arg audioFilePathArray, featureArray, csvWriteDirectory; var outputHeaders, track_extractedFeatures, csvContent,
    extractedFeatures;

    /* Invoke 'setPluginDirectories' */
    this.setGlobalDirectories;

    case
    /*  CASE - A transform folder exists... */
    {File.exists(transformDir)} {
      /* Make a List for holding the CSV's headers; indent by one index */
      outputHeaders = List.newUsing([""]);
      /* Make a MultiLevelIdentityDictionary to store a track and it's extracted features */
      track_extractedFeatures = MultiLevelIdentityDictionary();

      /* For all paths given... */
      audioFilePathArray.do({
        arg path, pathIteration;

        /* Replace all ',' inside a path name N.B. these are problematic when writing csv; Add a file's path to
        'csvContent'; add a comma separator*/
        csvContent = csvContent ++ PathName(path).fileNameWithoutExtension.replace(",") ++ ",";

        /* Extract features for path */
        extractedFeatures = this.extractFeatures(path, featureArray, false);

        /* For all the returned extracted features... */
        extractedFeatures.keysValuesDo({
          arg output, value;

          /* For the first feature result only; set the CSV's header...*/
          case {pathIteration == 0} {
            /* This loop makes the correct number of headings */
            value.do({
              arg item, valueIteration;

              /* Add an output to 'outputHeader' with its iteration */
              outputHeaders.add((output ++ " " ++ valueIteration));
            });
          };

          /* Convert an outputs corresponding value from a Array to a String; drop the Array's '[' and ']';
          add the resulting value to 'csvContent' */
          csvContent = csvContent ++ value.asString.drop(1).drop(-1) ++ ",";
        });

        /* Drop the final comma from 'csvContent'; start a new line (all results for 'path' are concatenated) */
        csvContent = csvContent.drop(-1) ++ "\n";

        /* Add to 'artist_track_summary' */
        track_extractedFeatures.put(PathName(path).fileNameWithoutExtension, extractedFeatures);
      });

      /* With all rows concatenated in 'csvContent', convert 'outputHeaders' from a Array to a String; drop the
      Array's '[' and ']'; add all the rows concatenated in 'csvContent' after */
      csvContent = outputHeaders.asArray.asCompileString.drop(1).drop(-1).replace("\"") ++ "\n" ++ csvContent;

      case
      /*  CASE - A csvWriteDirectory was provided... */
      {csvWriteDirectory.notNil} {
        /* Write the concatenated string to file in the given directory with the extension '.csv' */
        File(csvWriteDirectory ++ "SonicAnnotatorBatchResults.csv","w").write(csvContent).close;
      } /*  DEFAULT - no csvWriteDirectory was provided... */ {
        "No csvWriteDirectory was provided. Returning the CSVs content.\n".postln;
        ^track_extractedFeatures.dictionary;
      };
    }
  }
}