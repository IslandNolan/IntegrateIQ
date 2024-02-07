# IntegrateIQ
Technical Interview Repository which integrates to a Hubspot account

 ## Build and Install

Make sure you have the following dependencies installed:

- [Java 17+](https://www.oracle.com/java/technologies/downloads/#java17)
- [Maven 3.8+](https://maven.apache.org/download.cgi?)

Clone the repository, and navigate to the root of the project

In order to specify credentials you must place a file named `settings.json` in /resources/ before you compile. There is no 
cli support for setting a token. 

Example of `settings.json`:
```json
{
  "awsBearerAuthToken": "7642xxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "hubspotAuthToken": "pat-xxx-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```


After you have created a settings file, and added your credentials navigate to the root of the project and run
```mvn clean install -U```

This will produce two artifacts under /target/, both of which are very similar in name.
Only use the artifact that has the suffix "-shaded.jar".\
This is an uber jar which contains all the resources necessary to successfully run the program.

Once you have compiled the .jar, and identified the right artifact, simply run
``java -jar {jarfile}``
