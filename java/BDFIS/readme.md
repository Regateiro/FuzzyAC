The following command can be executed from the project root folder to run the BDFIS with the sample academic.fcl file:

java -jar release/BDFIS.jar academic.fcl Number_Of_Publications=12 Number_Of_Citations=50 [verbose] [chart]

The verbose option will print the parsed BDFIS from the FCL, the input values provided, and the inference calculation steps.
The chart option show charts for every step of the inference process.

Sample output:
```
FuzzyAC/java/BDFIS$ java -jar release\BDFIS.jar academic.fcl Number_Of_Publications=12 Number_Of_Citations=50 verbose

********* PARSE *********
FUNCTION_BLOCK AccessControl

VAR_INPUT
        Expertise : REAL;
END_VAR

VAR_OUTPUT
        Read : REAL;
        Write : REAL;
END_VAR

FUZZIFY Expertise
END_FUZZIFY

DEFUZZIFY Read
        TERM Deny :=  0.0;
        TERM Grant :=  1.0;
        METHOD : COGS;
        DEFAULT := NC;
        RANGE := (0.0 .. 1.0);
END_DEFUZZIFY

DEFUZZIFY Write
        TERM Deny :=  0.0;
        TERM Grant :=  1.0;
        METHOD : COGS;
        DEFAULT := NC;
        RANGE := (0.0 .. 1.0);
END_DEFUZZIFY

RULEBLOCK Read
        ACT : MIN;
        ACCU : MAX;
        AND : MIN;
        RULE 0 : IF Expertise IS Low THEN Read IS Deny;
        RULE 1 : IF Expertise IS Medium THEN Read IS Grant;
        RULE 2 : IF Expertise IS High THEN Read IS Grant;
        RULE 3 : IF Expertise IS Very_High THEN Read IS Grant;
END_RULEBLOCK
RULEBLOCK Write
        ACT : MIN;
        ACCU : MAX;
        AND : MIN;
        RULE 0 : IF Expertise IS Low THEN Write IS Deny;
        RULE 1 : IF Expertise IS Medium THEN Write IS Deny;
        RULE 2 : IF Expertise IS High THEN Write IS Deny;
        RULE 3 : IF Expertise IS Very_High THEN Write IS Grant;
END_RULEBLOCK

END_FUNCTION_BLOCK

FUNCTION_BLOCK VariableInference

VAR_INPUT
        Number_Of_Citations : REAL;
        Number_Of_Publications : REAL;
END_VAR

VAR_OUTPUT
        Expertise : REAL;
END_VAR

FUZZIFY Number_Of_Citations
        TERM Considerable :=  (4.0, 0.0) (10.0, 1.0) (40.0, 1.0) (80.0, 0.0) ;
        TERM High :=  (30.0, 0.0) (100.0, 1.0) ;
        TERM Low :=  (0.0, 1.0) (4.0, 1.0) (10.0, 0.0) ;
END_FUZZIFY

FUZZIFY Number_Of_Publications
        TERM Considerable :=  (3.0, 0.0) (5.0, 1.0) (13.0, 1.0) (18.0, 0.0) ;
        TERM High :=  (10.0, 0.0) (15.0, 1.0) ;
        TERM Low :=  (3.0, 1.0) (5.0, 0.0) ;
END_FUZZIFY

DEFUZZIFY Expertise
        TERM High :=  3.0;
        TERM Low :=  1.0;
        TERM Medium :=  2.0;
        TERM Very_High :=  4.0;
        METHOD : COGS;
        DEFAULT := NC;
        RANGE := (1.0 .. 4.0);
END_DEFUZZIFY

RULEBLOCK Expertise
        ACT : MIN;
        ACCU : MAX;
        AND : MIN;
        RULE 0 : IF (Number_Of_Publications IS Low) AND (Number_Of_Citations IS Low) THEN Expertise IS Low;
        RULE 1 : IF (Number_Of_Publications IS Low) AND (Number_Of_Citations IS Considerable) THEN Expertise IS High;
        RULE 2 : IF (Number_Of_Publications IS Low) AND (Number_Of_Citations IS High) THEN Expertise IS Very_High;
        RULE 3 : IF (Number_Of_Publications IS Considerable) AND (Number_Of_Citations IS Low) THEN Expertise IS Low;
        RULE 4 : IF (Number_Of_Publications IS Considerable) AND (Number_Of_Citations IS Considerable) THEN Expertise IS Medium;
        RULE 5 : IF (Number_Of_Publications IS Considerable) AND (Number_Of_Citations IS High) THEN Expertise IS High;
        RULE 6 : IF (Number_Of_Publications IS High) AND (Number_Of_Citations IS Low) THEN Expertise IS Low;
        RULE 7 : IF (Number_Of_Publications IS High) AND (Number_Of_Citations IS Considerable) THEN Expertise IS Medium;
        RULE 8 : IF (Number_Of_Publications IS High) AND (Number_Of_Citations IS High) THEN Expertise IS Very_High;
END_RULEBLOCK

END_FUNCTION_BLOCK



********* INPUT *********
{Number_Of_Citations=50.0, Number_Of_Publications=12.0}

******* INFERENCE *******
 - BLOCK: VariableInference
 |
 |-- INPUT: Number_Of_Citations (50.000000)
 | |-- TERM: High (0.285714)
 | |-- TERM: Low (0.000000)
 | |-- TERM: Considerable (0.750000)
 |
 |-- INPUT: Number_Of_Publications (12.000000)
 | |-- TERM: High (0.400000)
 | |-- TERM: Low (0.000000)
 | |-- TERM: Considerable (1.000000)
 |
 |-- OUTPUT: Expertise (2.648649)
 | |-- TERM: High (0.285714)
 | |-- TERM: Low (0.000000)
 | |-- TERM: Medium (0.750000)
 | |-- TERM: Very_High (0.285714)

 - BLOCK: AccessControl
 |
 |-- INPUT: Expertise (2.648649)
 | |-- TERM: High (0.285714)
 | |-- TERM: Low (0.000000)
 | |-- TERM: Medium (0.750000)
 | |-- TERM: Very_High (0.285714)
 |
 |-- OUTPUT: Read (1.000000)
 | |-- TERM: Grant (0.750000)
 | |-- TERM: Deny (0.000000)
 |
 |-- OUTPUT: Write (0.275862)
 | |-- TERM: Grant (0.285714)
 | |-- TERM: Deny (0.750000)

******** RESULTS ********
[Read] permission is GRANTED.
[Write] permission is DENIED.
```
