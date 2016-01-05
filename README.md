# jira-library
A user-friendly JIRA Client

## Instructions (Build from source)

### Prerequisites
* A *working installation* of Maven (and knowledge of using Maven)

### How to Build
* Checkout the repository
* Type `mvn install`

This creates a JIRA and stores it where Maven usually stores JARs.
You should see some output as seen below, which tells you where the JAR is:

    [INFO] Installing D:\path\to\jiralib\target\jiralib-0.0.2.jar to C:\Users\vish\.m2\repository\com\vish\jiralib\0.0.2\jiralib-0.0.2.jar
    [INFO] Installing D:\path\to\jiralib\pom.xml to C:\Users\vish\.m2\repository\com\vish\jiralib\0.0.2\jiralib-0.0.2.pom

### How to run
Create a `jira.properties` file in the location of the JAR.
This needs to contain the following properties:
  
  jiraurl=https://your-jira.com
  jirausername=your_jira_username
  jirapwd=your_jira_password
  jiraproject=Key_to_a_JIRA_project

