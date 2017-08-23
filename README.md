# AisCoverageDataExporter

### Introduction

AisCoverageDataExporter is a tool for exporting the data gathered by [AisCoverage](https://github.com/johnmartel/AisCoverage) to json and KML. First, it establishes a connection to the mongo database used to persist the coverage data, then, it loops through all the records and load their content. Finally, it exports it to json and/or KML, depending on the program arguments. Since this was made for the particular needs of the Canadian Coast Guard, only the needed features are implemented (e.g. only terrestrial messages are exported, not satellite messages). When the amount of coverage data is considerable, the heap size of the java virtual machine running AisCoverageDataExporter should be increased. This can be done using the -Xmx VM option when launching the program.
