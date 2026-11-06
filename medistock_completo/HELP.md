# Getting Started

## Local configuration

The application reads database, OAuth, and Groq credentials from environment
variables. Do not commit real credentials. In PowerShell, set the values before
starting the app:

```powershell
$env:DB_PASSWORD = "your-database-password"
$env:GOOGLE_CLIENT_ID = "your-google-client-id"
$env:GOOGLE_CLIENT_SECRET = "your-google-client-secret"
$env:GITHUB_CLIENT_ID = "your-github-client-id"
$env:GITHUB_CLIENT_SECRET = "your-github-client-secret"
$env:GROQ_API_KEY = "your-groq-api-key"

.\mvnw.cmd spring-boot:run
```

The OAuth credentials may be left as their `configure-me` defaults if OAuth
login is not being used. The chatbot key is optional until the chatbot is
configured. Environment variables apply only to the current terminal session.

### Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.1.1/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.1.1/maven-plugin/build-image.html)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/4.1.1/reference/actuator/index.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.1.1/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [OAuth2 Client](https://docs.spring.io/spring-boot/4.1.1/reference/web/spring-security.html#web.security.oauth2.client)
* [Spring Security](https://docs.spring.io/spring-boot/4.1.1/reference/web/spring-security.html)
* [Thymeleaf](https://docs.spring.io/spring-boot/4.1.1/reference/web/servlet.html#web.servlet.spring-mvc.template-engines)
* [Spring Web](https://docs.spring.io/spring-boot/4.1.1/reference/web/servlet.html)

### Guides

The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)
* [Handling Form Submission](https://spring.io/guides/gs/handling-form-submission/)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM. While most of the inheritance is
fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent. To prevent this, the
project POM contains empty overrides for these elements. If you manually switch to a different parent and actually want
the inheritance, you need to remove those overrides.
