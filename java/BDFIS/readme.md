The following command can be executed from the project root folder to run the BDFIS with the sample academic.fcl file:

java -jar release/BDFIS.jar academic.fcl Number_Of_Publications=12 Number_Of_Citations=50 [verbose] [chart]

The verbose option will print the parsed BDFIS from the FCL, the input values provided, and the inference calculation steps.
The chart option show charts for every step of the inference process.
