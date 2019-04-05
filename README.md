# GCOPDD

![Travis CI Status](https://travis-ci.org/vakabus/gcopdd.svg?branch=master)

= Graal Compiler Optimization Phases Data Dumper

## Description of repository structure

* `graal`, `mx` and `PLuG` subdirectories are dependencies included as submodules
* `blood` is our code specifying instrumentation. Code from here is compiled and injected into Graal compiler.
* `tests` contains simple test applications we can test the compiler on
* `docs` is for documentation

## Dependencies

To use this project, you have to have these tools installed:

* Java 11
* GNU Make
* Python 2.7

## Running Java with instrumented Graal compiler

```sh
make build
./vm {JAVA_ARGS}
```

This command can be run in cleanly cloned repository. Everything necessary will be built and in the end executed. When run again without changes to `blood` code, results from last builds will be reused.

## Example

For example, you can run this to print 17th fibbonaci number:

```sh
./vm tests/Fibbonaci.java
```
