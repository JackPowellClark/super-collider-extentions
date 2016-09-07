/*
* CSVFileNormaliser.sc
* Purpose: Takes a CSV file of values and normalises them
*
* @author: Jack Clark
* @version: 1.0
*
* @info: Built in SuperCollider 3.6.6
* @line length limit: 120
*/

CSVFileNormaliser {
  *new {
    arg csvPath, writePath; var csv, newColumns, csvContent;

    csv = CSVFileReader.read(csvPath);
    newColumns = List();

    (csv[0].size).do({
      arg columnIteration; var values;

      values = List();
      (csv.size - 1).do({
        arg rowIteration;

        values.add(csv[rowIteration + 1][columnIteration]);
      });

      case {columnIteration == 0} {
        newColumns.add([csv[0][columnIteration]] ++ values.asArray);
      } {
        newColumns.add([csv[0][columnIteration]] ++ values.asArray.asFloat.normalize.round(0.001));
      }
    });

    csv.size.do({
      arg lineIteration; var newRow;

      newRow = List();

      newColumns.do({
        arg newColumn;

        newRow.add(newColumn[lineIteration]);
      });

      csvContent = csvContent ++ newRow.asArray.asCompileString.drop(2).drop(-2).replace("\"") ++ "\n"
    });

    File(writePath,"w").write(csvContent).close;
  }
}