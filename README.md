# OpenConext-pdp

OpenConext implementation of a XACML based PDP engine for access policy enforcement including a GUI for maintaining policies

## Getting started

### System Requirements

- Java 8
- Maven 3
- MySQL 5.5
- Gruntjs

### Create database

Connect to your local mysql database: `mysql -uroot`

Execute the following:

```sql
CREATE DATABASE `pdp-server` DEFAULT CHARACTER SET latin1;
create user 'pdp-serverrw'@'localhost' identified by 'secret';
grant all on `pdp-server`.* to 'pdp-serverrw'@'localhost';
```

## Building and running

### The pdp-server

This project uses Spring Boot and Maven. To run locally, type:

`cd pdp-server`

`mvn spring-boot:run -Drun.jvmArguments="-Dspring.profiles.active=dev"`

When developing, it's convenient to just execute the applications main-method, which is in [PdpApplication](pdp-server/src/main/java/pdp/PdpApplication.java). Don't forget
to set the active profile to dev otherwise the application uses the real VOOT client on the test environment.

### The pdp-gui

The client is build with react.js and to get initially started:

`cd pdp-gui`

`brew install npm;`

`gem install sass;`

`gem install sass-globbing;`

`gem install compass;`

`npm install -g grunt-cli;`

`npm install;`

When new grunt dependencies are added:

`npm install`

To build:

`grunt watch`

To run locally:

`grunt server`

When you browse to the [application homepage](http://localhost:8001/) you will be prompted for a login. Anything - for now - is accepted.

## Miscellaneous

### Design considerations

The XACML framework works with policies defined in XML. We store the policies as XML strings in the database. However to
effectively let XACML evaluate policies we need to convert them to the internal XACML format - see [OpenConextEvaluationContextFactory](pdp-server/src/main/java/pdp/xacml/OpenConextEvaluationContextFactory.java).

Working with XML on the pdp-gui does not work well and we want to keep the pdp-gui simple. Therefore the PdpPolicyDefinition is used as an
intermediate format for policies that is easy to work with for the pdp-gui and also enables the server to transform
it easily into the desired - yet very complex - XML format.

Using the internal XACML Policy class hierarchy for communication back and forth with the client was not an option because
of the cyclic dependencies in the hierarchy (and not desirable because of the complexity it would have caused).

### Local database content

We don't provide flyway migrations to load initial policies. 

However if you start up the application with the spring.profiles.active=dev then all the policies
in the folder `OpenConext-pdp/pdp-server/src/main/resources/xacml/policies` are added to the database. Do note that any other policies already in the database are deleted.

### Testing

There are integration tests for PdpApplication that tests the various decisions against a running Spring app. See [PdpApplicationTest](pdp-server/src/test/java/pdp/PdpApplicationTest.java)

If you want to test individual Policies with specific Request / Response JSON then use the (very fast) [StandAlonePdpEngineTest](pdp-server/src/test/java/pdp/StandAlonePdpEngineTest.java)

### Configuration and Deployment

On its classpath, the application has an [application.properties](pdp-server/src/main/resources/application.properties) file that
contains configuration defaults that are convenient when developing.

When the application actually gets deployed to a meaningful platform, it is pre-provisioned with ansible and the application.properties depends on
environment specific properties in the group_vars. See the project OpenConext-deploy and the role pdp for more information.

For details, see the [Spring Boot manual](http://docs.spring.io/spring-boot/docs/1.2.1.RELEASE/reference/htmlsingle/).

