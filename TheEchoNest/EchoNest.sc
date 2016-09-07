 /*
* EchoNest.sc
* Purpose: A wrapper for accessing The Echo Nest's track API methods
*
* @author: Jack Clark
* @version: 1.0
* @info: Built in SuperCollider 3.6
* @line length limit: 120
*/

TheEchoNest {
  /* Environment Variables; variables that are required to be accessible in every scope */
  classvar pendingMessageShown = false;

  /*
  * Build an upload query for The Echo Nest
  *
  * @param path: An audio track that must be of type wav, mp3, au, ogg, m4a or mp4
  * @param developerAPIKey: A user's Developer API Key for the Echo Nest
  * @param tmpDir: A temporary directory that will hold an upload queries response
  * @param tmpFileName: A temporary file name for the upload queries response
  *
  * @return: An upload query
  */
  *buildUploadQuery {
    arg path, developerAPIKey, tmpDir, tmpFileName = 1; var uploadQuery;

    /* Build an upload query for The Echo Nest */
    uploadQuery = ("curl -o " ++ (tmpDir ++ tmpFileName ++ ".txt").escapeChar($ ) ++ " -X POST -H " ++
      ("Content-Type:application/octet-stream").quote	++ " " ++
      ("http://developer.echonest.com/api/v4/track/upload?api_key=" ++ developerAPIKey ++ "&filetype=" ++
        PathName(path).extension.toLower).quote	++ " --data-binary " ++
      ("@" ++ path.escapeChar($ ).escapeChar($').escapeChar($&).escapeChar($().escapeChar($))).quote.quote ++ " &");

    /* Return the query */
    ^uploadQuery;
  }

  /*
  * Build an ID query for The Echo Nest
  *
  * @param id: A track id compatible with The Echo Nest
  * @param developerAPIKey: A user's Developer API Key for the Echo Nest
  * @param tmpDir: A temporary directory that will hold an upload queries response
  * @param tmpFileName: A temporary file name for the upload queries response
  *
  * @return: An ID query
  */
  *buildIdQuery {
    arg id, developerAPIKey, tmpDir, tmpFileName = 1; var idQuery;

    /* Build an upload query for The Echo Nest */
    idQuery = ("curl -o " ++ (tmpDir ++ tmpFileName ++ ".txt").escapeChar($ )
      ++ " " ++ (("http://developer.echonest.com/api/v4/track/profile?api_key=") ++ developerAPIKey ++
        "&format=json&id=" ++ id ++ "&bucket=audio_summary").quote) ++ " &";

    /* Return the query */
    ^idQuery;
  }

  /*
  * Upload a track to The Echo Nest's analyzer for analysis; optionally query for results once complete
  *
  * @param path: An array of audio paths
  * @param developerAPIKey: A user's Developer API Key for The Echo Nest
  * @param query: Boolean indicating whether to invoke 'idQuery' once uploads are complete
  * @param csvWriteDirectory: The directory in which to write results to
  *
  * @return: A parsed IdentityDictionary of form Artist -> Title -> Audio Summary (if 'query' is true) OR an Array of track IDs (if 'query' is false)
  */
  *pathQuery {
    arg pathArray, developerAPIKey, query = true, csvWriteDirectory; var pS, tmpDir, scriptName, script, scriptContents, returnedIDs;

    /* Set a variable to hold the platform specific path separator */
    pS = Platform.pathSeparator;
    /* Get a user's default temporary directory; add to it */
    tmpDir = (Platform.userExtensionDir ++ pS ++ "SuperColliderExtensions" ++ pS ++ "TheEchoNest" ++ pS ++ "tmp" ++ pS);
    /* Make a script name */
    scriptName = "uploadToTheEchoNest.sh";
    /* Make a tmp file for holding a bash script; open it */
    script = File(Platform.defaultTempDir ++ scriptName, "w");
    /* Set the variable for holding the script contents; start with a directory check */
    scriptContents = "if [ ! -d " ++ tmpDir.escapeChar($ ) ++ " ]\nthen\nmkdir " ++ tmpDir.escapeChar($ ) ++
    "\nfi\n\n";

    /* For all the audio paths given... */
    pathArray.do({
      arg path, iteration;

      /* Get the current script contents; add a path query to it */
      scriptContents = scriptContents ++ this.buildUploadQuery(path, developerAPIKey, tmpDir, iteration.asString)
      ++ "\n";
    });

    /* Get the variable holding script contents; add a 'wait' onto the end */
    scriptContents = scriptContents + "\nwait";
    /* Write to the tmp file */
    script.write(scriptContents);
    /* Close the file */
    script.close;

    /* Post helpful information */
    ("Uploading tracks to The Echo Nest's analyzer for analysis.").postln; ("").postln;

    /* Post a warning message */
    ("Please be patient; depending on the size of the files requested and the speed of your available ").post;
    ("Internet connection this process can take a considerably long time.").postln; ("").postln;

    /* Post helpful information */
    ("A post message will indicate when this process has completed.").postln;

    /* Set the permissions for the created script; run the script */
    ("cd " ++ Platform.defaultTempDir ++ "; chmod +x " ++ scriptName ++ "; ./" ++ scriptName).unixCmdGetStdOut;

    /* Post when an upload is complete */
    ("").postln; ("*** Upload complete ***").postln; ("").postln;

    /* Make a List to store returned IDs */
    returnedIDs = List();
    /* For all newly returned upload responses... */
    (tmpDir ++ "*.txt").pathMatch.do({
      arg path; var response, id;

      /* Open the txt file; read it */
      response = File(path, "r");
      /* Parse it; get the response ID */
      id = response.readAllString.parseYAML.at("response").at("track").at("id");
      /* Close the file */
      response.close;

      /* Add the response ID to the 'returnedIDs' List */
      returnedIDs.add(id);
    });

    /* All IDs retrieved; delete tmp dir */
    ("rm -rf " ++ (tmpDir.quote)).unixCmd;

    /* CASE - 'query' set to true... */
    case
    {query = true}{
      /* Invoke 'idQuery'; querying The Echo Nest with a dictionary of paths & track IDs */
      ^this.idQuery(returnedIDs.asArray, developerAPIKey, csvWriteDirectory);
    } /* DEFAULT - 'query' is not set to true... */ {
      /* Post helpful information */
      ("Posting and returning upload IDs:").postln; returnedIDs.asArray.posltn; ("").postln;
      /* Return IDs */
      ^returnedIDs.asArray;
    }
  }

  /*
  * Query The Echo Nest with a track ID
  *
  * @param echoNestID: A track ID compatible with The Echo Nest
  * @param developerAPIKey: A user's Developer API Key for the Echo Nest
  * @param csvWriteDirectory: The directory in which to write results to
  *
  * @return: A parsed dictionary of form Artist -> Title -> Audio Summary
  */
  *idQuery {
    arg idArray, developerAPIKey, csvWriteDirectory; var pS, tmpDir, scriptName, script, scriptContents, parsedResponse,
    analysisStatus, artist_track_summary, attributeHeaders, csvContent;

    /* Set a variable to hold the platform specific path separator */
    pS = Platform.pathSeparator;
    /* Get a user's default temporary directory; add to it */
    tmpDir = (Platform.userExtensionDir ++ pS ++ "SuperColliderExtensions" ++ pS ++  "TheEchoNest" ++ pS ++ "tmp" ++ pS);
    /* Make a script name */
    scriptName = "queryTheEchoNest.sh";
    /* Make a tmp file for holding a bash script; open it */
    script = File(Platform.defaultTempDir ++ scriptName, "w");
    /* Set the variable for holding the script contents; start with a directory check */
    scriptContents = "if [ ! -d " ++ tmpDir.escapeChar($ ) ++ " ]\nthen\nmkdir " ++ tmpDir.escapeChar($ ) ++
    "\nfi\n\n";

    /* For all the track IDs given... */
    idArray.do({
      arg id, iteration;

      /* Get the current script contents; add an id query to it */
      scriptContents = scriptContents ++ this.buildIdQuery(id, developerAPIKey, tmpDir, iteration.asString) ++ "\n";
    });

    /* Get the variable for holding script contents; add a 'wait' onto the end */
    scriptContents  = scriptContents + "\nwait";
    /* Write to the tmp file */
    script.write(scriptContents);
    /* Close the file */
    script.close;

    /* Post helpful information */
    ("Querying The Echo Nest with track IDs.").postln; ("").postln;

    /* Set the permissions for script; run the script */
    ("cd " ++ Platform.defaultTempDir ++ "; chmod +x " ++ scriptName ++ "; ./" ++ scriptName).unixCmdGetStdOut;

    /* Post when a query is complete */
    ("").postln; ("*** Query complete ***").postln; ("").postln;

    /* Make a List for holding the CSV's headers; indent by one index */
    attributeHeaders = List.new();
    /* Make a MultiLevelIdentityDictionary to store an artist's tracks and their audio summaries */
    artist_track_summary = MultiLevelIdentityDictionary();
    /* For all the newly returned query responses... */
    (tmpDir ++ "*.txt").pathMatch.do({
      arg path, pathIteration; var response, trackProfile, artist, track, audio_summary;

      /* Open the txt file; read it */
      response = File(path, "r");
      /* Parse it */
      trackProfile = response.readAllString.parseYAML;
      /* Get the track artist */
      artist = trackProfile.at("response").at("track").at("artist");
      /* Get the track title */
      track = trackProfile.at("response").at("track").at("title");
      /* Get the track audio summary */
      audio_summary = trackProfile.at("response").at("track").at("audio_summary");

      /* For the first feature result only; set the CSV's 'Track' and 'Artist' headers */
      case
      /*  CASE - the current path is the first... */
      {pathIteration == 0} {
        /* Add an output to 'attributeHeader' with its iteration */
        attributeHeaders.add("Track").add("Artist");
      };
      /* Add 'track' and 'artist' to 'csvContent' */
      csvContent = csvContent ++ track.replace(",") ++ "," ++ artist.replace(",") ++ ",";

      /* For all attributes in 'audio_summary' */
      audio_summary.keysValuesDo({
        arg attribute, value;

        case
        {attribute != "analysis_url"}{
          /* For the first feature result only; set the CSV's attribute headers */
          case
          /*  CASE - the current path is the first... */
          {pathIteration == 0} {
          /* Add an attribute to 'attributeHeader' with its iteration */
            attributeHeaders.add(attribute);
          };
          /* Add an attributes value to 'csvContent' */
          csvContent = csvContent ++ value.asFloat.round(0.00001) ++ ",";
        }
      });

      /* Drop the final comma from 'csvContent'; start a new line (all results for 'path' are concatenated) */
      csvContent = csvContent.drop(-1) ++ "\n";

      /* Add to 'artist_track_summary' */
      artist_track_summary.put(artist, track, audio_summary);
    });

    /* With all rows concatenated in 'csvContent', convert 'attributeHeaders' from a Array to a String; drop the
    Array's '[' and ']'; add all the rows concatenated in 'csvContent' after */
    csvContent = attributeHeaders.asArray.asCompileString.drop(1).drop(-1).replace("\"") ++ "\n" ++ csvContent;

    /* All query responses retrieved; delete tmp dir */
    ("rm -rf " ++ (tmpDir.quote)).unixCmd;

    case
    /*  CASE - A csvWriteDirectory was provided... */
      {csvWriteDirectory.notNil} {
      /* Write the concatenated string to file in the given directory with the extension '.csv' */
      File(csvWriteDirectory ++ "TheEchoNestBatchResults.csv","w").write(csvContent).close;
    } /*  DEFAULT - no csvWriteDirectory was provided... */ {
      ("No csvWriteDirectory was provided. Returning a dictionary of results.\n").postln;
      /* Convert 'artist_track_summary' to an Identity Dictionary; return it */
      ^artist_track_summary.dictionary;
    };
  }
}