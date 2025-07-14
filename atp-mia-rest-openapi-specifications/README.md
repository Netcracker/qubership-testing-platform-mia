#How to add new endpoint
1) Add classic spring endpoint to .java controller file
e.g. `atp-mia-backend/src/main/java/com/qubership/atp/mia/controllers/ExecutionHelperController.java`
2) Add endpoint description to .yaml file
e.g. in file `atp-mia-rest-openapi-specifications/v1/execution-helper-controller-v1.yaml`
3) Build whole project via Maven: `maven clean verify` (better not to skip test to see possible errors).
4) After that open api tests won't fail.
P.S. to make sure target build contains check file: 
`atp-mia-backend/target/generated-sources/openapi/src/main/java/org/qubership/atp/mia/controllers/api/ExecutionHelperControllerApi.java`
#How to validate config
Just drag&drop .yaml file to web-page https://editor.swagger.io/
#In case of errors
If you get error such as:
```
java.lang.RuntimeException: Found new or not excluded method 'getGridFsSize' for class interface org.qubership.atp.mia.controllers.api.ExecutionHelperControllerApi
```
try to rebuild whole maven project.
