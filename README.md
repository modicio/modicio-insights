# codi-insights

## About

This application is a minimal web starter written in Scala to odemonstrate the codi-native framework. This project was initially contributed as a part of the master thesis of Karl Kegel (KKegel) *Deep Models at Runtime for User-Driven Flexible Systems ([full text](https://www.researchgate.net/publication/361725823_Deep_ModelsRuntime_for_User-Driven_Flexible_Systems))* at TU-Dresden. For more detailled background information check the thesis and the codi-native documentation and wiki.

The main idea of this project is to provide a runnable and minimal application to demonstrate the framework functionality. Therefore, codi-insights provides a model view including a model editor for codi-native deep-models and an user view to simulate an application frontend to use those models at runtime. Between model updates, no downtime, data-scheme or code-changes are required.

> :bulb: Note: No verificators are implemented in this demo-application. This includes checks of rules and attribute values and association multiplicities. Example 1: a nonesensical link-rule input is not rejected but accepted and ignored. Example 2: An attribute is modelled as obligatory number but any string input, also empty strings, are accepted.

Most functionality regarding codi-native is further explained and extracted as part of our [getting-started guide](https://github.com/modicio/codi-native/wiki/Getting-Started)

## Release Notes

This case-study application contains releases to follow the revision of codi-native:

* [0.1a - experiment](https://github.com/modicio/codi-insights/releases/tag/0.1a) contains the version used to execute the experiment/user-study of Karl Kegel's master thesis linked above.
* [0.1.2 - MULTI]() contains the version applying newer concepts from the paper submission MULTI 2022. This includes: 
   * Modelling of model-space instance values
   * Creation of singleton-instances
   
Newer commits which are not yet part of a release may fix minor bugs or enhance documentation. We recommend to use the latest commit for exploration.

## Getting Started

### Start from CI Build

The CI pipeline of this repository creates a dist package with a runnable bash script. You can download the package by clicking on the latest CI run. 
* Unpack the ZIP
* Place the resources folder of this repos inside the bin folder
* Execute the codi-native bash file inside bin on your terminal. 

After some time, the web-app is available on localhost:9000 (or different port, check the terminal output).

> :zap: Windows Powershell may not work, please use a true unix bash, for example GitBash or Ubuntu.

A running java setup is required to run this application, the dist requires OpenJDK 11 or newer, a JRE may be sufficent.

> :ambulance: If the last CI build is older then some Github defaults, the build artifacts may be unavailable. In this case please use the custom build explained below.

###  Custom Build

* Clone this repository
* Install SBT (Scala build system) and a JDK (14+ is tested, 11+ may work)
* Go to the root directory where this README and the ``build.sbt`` is located.
* Execute ``sbt dist`` and check the terminal output where the generated JAR package is located
* Alternatively, execute ``sbt run`` directly to start the app without generating a JAR

### Workflows and Explanations

For further explanations, check the linked thesis, here especially the chapter *Evaluations/Case-Study*. For an example workflow, check the **Experiment Instructions** part of the appendix. **An example workflow to try this application is given in [ExampleWorkflow](https://github.com/modicio/codi-insights/blob/main/ExampleWorkflow.pdf)**.
