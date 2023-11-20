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
--project-path (required)
Path to the analyzed project's root.

--db-url (optional)
Database path to store version numbers and their release dates.
Expected format: jdbc:sqlite:identifier.sqlite
```



---
[1] J. Cox, E. Bouwers, M. van Eekelen and J. Visser, Measuring Dependency Freshness in Software
Systems. In Proceedings of the 37th International Conference on Software Engineering (ICSE 2015),
May 2015 https://ericbouwers.github.io/papers/icse15.pdf \
[2] https://github.com/oss-review-toolkit/ort