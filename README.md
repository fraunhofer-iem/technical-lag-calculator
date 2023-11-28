# Libyear-ORT

This CLI tool calculates a project's libyear[1] score. The libyear score describes the age of a 
software's dependencies by measuring the difference between the release date of the newest version
and the currently used version.\
Our tool uses the ORT[2] to get a list of all used dependencies in the analyzed project. Therefore,
we theoretically support all ecosystems currently implemented in the ORT. In the current version,
we fully support Maven. To add a new ecosystem to our tool we need to implement the code to gather
the release date information for the new ecosystem. 

### Usage
Build the tool using gradle and run with the following command line options:
```
Options:
  optional:
  --db-url=<text>        Optional path to store a file based database which
                         contains version numbers and their release dates.This
                         database is used as a cache and the application works
                         seamlessly without it.If the path doesn't exist it
                         will be created.
  --user-name=<text>     Username of database user
  --password=<text>      Password for given database user
  --output-path=<path>   Path to the folder to store the JSON resultsof the
                         created dependency graph. If the path doesn't exist it
                         will be created.
  required:                       
  --project-path=<path>  Path to the analyzed project's root.
  

  -h, --help             Show this message and exit
```
Instead of using the CLI parameters the db url and project path can also be set using the
environment variables `PROJECT_PATH` and `DB_URL`.

### Maintainer
* Jan-Niclas Strüwer

---
[1] J. Cox, E. Bouwers, M. van Eekelen and J. Visser, Measuring Dependency Freshness in Software
Systems. In Proceedings of the 37th International Conference on Software Engineering (ICSE 2015),
May 2015 https://ericbouwers.github.io/papers/icse15.pdf \
[2] https://github.com/oss-review-toolkit/ort