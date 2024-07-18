# Technical Lag Calculator

This CLI tool calculates a project's libyear[1] score. The libyear score describes the age of a 
software's dependencies by measuring the difference between the release date of the newest version
and the currently used version. Further, we calculate additional information like the release distance, 
number of missed releases, release frequency, and more. \
Our tool uses the ORT[2] to get a list of all used dependencies in the analyzed project and deps.dev to 
retrieve further information about the analyzed dependencies. Therefore,
we theoretically support all ecosystems currently implemented in the ORT and deps.dev. 

### Usage

For development purposes, we recommend to import the project into IntelliJ and use one of the preconfigured 
run configurations to get started.
Currently, we support two different commands: \
`create-dependency-graph`, which creates the dependency graphs for the given projects and annotates the graph's 
nodes with their version information.\
`calculate-technical-lag`, which takes a list of annotated dependency graphs as an input and calculates and
stores their corresponding technical lag. \

Both commands take an input file in which a list of paths is stored for the projects to analyze or the dependency 
graph files respectivley. 
The files format looks as follows:
```
{
  "paths":[
    "path/to/my/input"
  ]
}

```

\

To run the technical lag calculator you can either use the included `Dockerfile` or build it using 
`./gradlew installDist` and then run the resulting artifact with `./build/install/libyear-ort/bin/technical-lag-calculator`.

### Architecture overview

![architecture](doc/workflow.png)

### Maintainer
* Jan-Niclas Strüwer

---
[1] J. Cox, E. Bouwers, M. van Eekelen and J. Visser, Measuring Dependency Freshness in Software
Systems. In Proceedings of the 37th International Conference on Software Engineering (ICSE 2015),
May 2015 https://ericbouwers.github.io/papers/icse15.pdf \
[2] https://github.com/oss-review-toolkit/ort
