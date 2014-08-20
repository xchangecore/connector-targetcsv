connector-targetcsv
===================

This connector monitors a directory for a csv file and transform each row of the csv file to XchangeCore incidents.

Dependencies:
connector-util
connector-async

To Build:
1. Use maven and run "mvn clean install" to build the dependencies.
2. Run "mvn clean install" to build geoRSSAdapter.

To Run:
1. Copy the targetcsv/src/main/resources/contexts/target-context to the same directory of the targetAdapter.jar file.
2. Use an editor to open the target-context file.
3. Look for the webServiceTemplate bean, replace the "defaultUri" to the XchangeCore you are using to run this adapter to create the incidents.
   If not localhost, change http to https, example "https://test4.xchangecore.leidos.com/uicds/core/ws/services"
4. Change the "credentials" to a valid username and password that can access your XchangeCore.
5. Change the inboundFileDirectory bean to the dirctory the connector will be monitoring for the csv file.
6. Open a cygwin or windows, change directory to where the SampleGeorssAdapter.jar file is located, run "java -jar targetAdapter.jar".

This connector can only consume a specific csv format.  A sample of the csv file is included in the sampledata directory.
