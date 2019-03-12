The following command can be executed from the project root folder to run the BDFIS with the sample academic.fcl file:

java -jar release/BDFISAuditor.jar academic.fcl permissions=Read,Write [permutate] [validate] [saveDecisions]

The permissions option indicates what permissions are to be audited.

Every FCL file provided has a Read permission, the academic files have an additional Write permission.

The permutate option makes the auditor check every input variable order permutation to allow the user to see how it affects the auditing algorithm efficacy.

The validate option forces the auditor to also audit the FCL file using a brute-force algorithm and compare the results to validate the optimized algorithm.

The saveDecisions option will make the auditor save the individual access control decisions for each input variable value combination into files, named \<fclFileName\>_\<permission\>.txt.
