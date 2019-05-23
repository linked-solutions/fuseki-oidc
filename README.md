# Fuseki OIDC

An [Apache Fuseki](http://jena.apache.org/documentation/fuseki2/index.html) extension and Docker distribution that provides 
OIDC based access control to Fuseki SPARQL Endpoint. The project is work in progress and currently known to work with the [Keycloak](https://www.keycloak.org/)
open source Identity and Access Management solution.

## Info

This Fuseki distribution allows to access the SPARQL Endpoints both using standard HTTP Basic-Auth and logging in as admin user
(its default password is `pw`) as well as by authenticating with a keycloak server.
This project provides ability to have flexible graph based permissions control for TDB2 datasets.   
Authorization is based on access-control definitions contained in graph specified in the configuration, by default in the graph `urn:fuseki-oidc:security`. Each user can have assigned read/write permissions to a concrete graph or to all graphs whose names match a specified [ANT style pattern](http://ant.apache.org/manual/dirtasks.html#patterns).
Also each user have complete access to it's "OWN" graph, the prefix for this graph can be specified. By default the graph is name as follows: `urn:fuseki-oidc:user:{username}`.
By default the admin user has ability to login through basic auth and has full access to every graph in dataset.


## To build application you will need:
* Docker

### Building

    docker build -t linkedsolutions/fuseki-oidc .

Note however that in most cases you won't need to build the docker image as this is provided via docker-hub.

### Running

If you just want to try out things on your local machine we recommend you use the provided docker-compose file and skip forward to the [Launching with docker-compose](#launching-with-docker-compose) section.

As a prerequisite to run fuseki-oidc you'll need an instance of keycloak running and configured, see the section [Keycloak setup](#keycloak-setup) below for information on how to set up keycloak.

Assuming your keycloak instance is running on `auth.example.org:8080` you can start fuseki-oidc with 

    docker run -e AUTH_SERVER_URL=http://auth.example.org:8080/auth -p 3030:3030 linkedsolutions/fuseki-oidc

__IMPORTANT__: The specified AUTH_SERVER_URL must both be the URL the end user sees during as well as be accessible by the fuseki instance, `localhost`-urls do not work as this will resolve to the docker container to fuseki.

To test the fuseki-oidc instance you just launched you may want to use the provided [sample-client](sample-client-app).

#### Launching with docker-compose

The provided `docker-compose.yml` serves as reference for launching Fuseki OIDC with [docker-compose](https://docs.docker.com/compose/) as well as to launch all required conatiners locally (for testing purposes).

As mentioned the above the authenticating server must be accessible under the same hostname both by the user as well as by fuseki and `localhost` doesn't work for this purpose. Because of this to use the docker-compose file as it is you'll need to add a host entry to your `/etc/hosts` file (`C:\Windows\System32\Drivers\etc\hosts` on windows).

Add the following line to have the hostname `keycloak` point to the local machine.

    127.0.0.1 keycloak

After adding this entry you can start fuseki oidc, keycloak and the sample client simply by executing

    docker-compose

First you'll want to access http://keycloak:8080/auth/admin/ to configure Keycloak as described inthe section [Keycloak setup](#keycloak-setup) below. Once keycloak is configured you may access the sample at http://localhost:8081/ and use https://keycloak:8080/auth/realms/master as authority and http://localhost:5030/ds/query / http://localhost:5030/ds/update as SPARQL endpoints. Do not use the admin user to test SPARQL (not sure why this currently doesn't work).

### Configuration

In productive settings you'll typically want to configure at least the admin password for fuseki, which is done with the `shiro.ini` file. Often wou'll also want to make changes to the fuseki configuration (that's the `config.ttl` file). Also you'll want to make changes to the security graph (changes made to this graph at runtime are currently not persistent).

The 3 mentioned files are located at the folowing locations within the fuseki-oidc container:

 - /usr/local/fuseki/shiro.ini
 - /usr/local/fuseki/config.ttl
 - /sec-data.ttl

When starting docker on unix/linux you may use `-v` parameter to replace a default configuration file, e.g:

    docker run -v `pwd`/conf/admin_security_data.ttl:/sec-data.ttl -e AUTH_SERVER_URL=http://auth.example.org:8080/auth -p 3030:3030 linkedsolutions/fuseki-oidc

The above doesn't work on windows where it is not possible to mount individual files. 

With docker-compose you can use the `volumes` directive to replace such a file, e.g.:

    volumes:
      - ./conf-fuseki-oidc/shiro.ini:/usr/local/fuseki/shiro.ini

The docker-compose versions works both with unix systems as well as on windows.

## Security configuration
There is one predefined security graph, the name can be configured and defaults to `<urn:fuseki-oidc:security>` 
It will contain information about user access rights to the other graphs. 
This graph contains instances of `acl:Authorization` that grant a user or a class of users
access to a graph or all graph with a name matching a specified pattern.

Fuseki-OIDC uses the acl Ontology defined at http://www.w3.org/ns/auth/acl# offering partial support with extensions defined in https://linked.solutions/fuseki-oidc/ontology# described in [ontology.ttl](./ontology.ttl).

      
## Keycloak setup
* Get the last docker image jboss/keycloak
* Run with `docker run -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin -p 8080:8080 jboss/keycloak`   
  username and password can be changed of course 
* Go to http://localhost:8080/auth/admin/, login as admin with credentials
* Open "Realm Settings" -> "Tokens" and setup default signature to RS-256
* Enable `User registration` in "Realm Settings" -> "Login"
  or just add any identity provider in "Identity Providers" page   
  _IMPORTANT_ you need to include `email` into "Default Scopes" of an identity provider
* setup new client for frontend, set name and url. 
  After it created add an email mapper in "Mappers" tab using "Add Builtin" button
  * Enable Implicit Flow for this client in its keycloak settings
* Library for frontend interaction with Keycloak can be found [here](https://www.npmjs.com/package/keycloak-js)

Additional information about [keycloak](https://www.keycloak.org/).  

## Thanks
Thanks to [SmartSwissParticipation](smartswissparticipation.com) for developing an initial version
