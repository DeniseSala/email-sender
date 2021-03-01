# Email Sender

Spring Boot + Kafka sample app that sends emails asynchronously.

### Requirements
* Java 11

### How to build
* `mvn clean install`

### How to run
* configure an external Kafka Broker and SMTP server in the application.properties file
* run with `./mvnw spring-boot:run`

### API documentation
* [Swagger](http://localhost:8080/swagger-ui.html)

### Notes
* for simplicity the email model only contains a single recipient, but it can be easily extended
* I created a test endpoint to test the attachment download from url (see AttachmentTestServer.java)
