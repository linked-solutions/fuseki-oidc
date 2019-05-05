# Fuseki OIDC

An [Apache Fuseki](http://jena.apache.org/documentation/fuseki2/index.html) extension and Docker ditribution that provides 
OIDC based access control. The project is work in progress and currently known to work with the [Keycloak](https://www.keycloak.org/)
open source Identity and Access Management solution.

## Info

This Fuseki distribution allows to access the SPARQL Enpoints both using standard HTTP Basic-Auth and logging in as admin user
(its default password is `pw`) as well as by authenticating with a keycloak server.
This project provides ability to have flexible graph based permissions control for TDB2 datasets   
Authorization is based on data that contains in graph `http://www.smartswissparticipation.com/security`
where each user can have assigned read/write permissions to a concrete graph or to all graphs whose name matches a specified [ANT style pattern](http://ant.apache.org/manual/dirtasks.html#patterns).
Also each user have complete access to it's "OWN" graph `http://www.smartswissparticipation.com/graphs/users/{username}`  
By default admin user has ability to login through basic auth and has full access to every graph in dataset


## To build application you will need:
* Docker

### Building

    docker build -t linkedsolutions:fuseki-oidc .

Note however that in most cases you won't need to build the docker image as this is provided via docker-hub.

[TODO rework the following:]

Put appropriate configs into src/main/resources/keycloack.json.
Most probably you will need to change just `auth-server-url`. See [Keycloak setup section](#keycloak-setup)   
It's __IMPORTANT__  to have `auth-server-url` that matches url that end users will see during login, 
as system checks issuer of the token to ensure that server is not changed.
So if running in local docker you will need to add ip of Keycloak server to /etc/hosts. 
Parameter --add-host=ip:hostname can be used on container start.
Same host should be used when logging in through browser so it also should be added to local hosts file
Also keep in mind ports redirecting - ports should be also the same from Fuseki container perspective and your browser perspective 

In conf/shiro.ini you can find section `[users]` that contains one admin user with password.
You will need to change the password to more secure. 
Only this one user will be able to login with Basic auth. 
Security graph is prepopulated with the data that allows access to every graph for this user 
so do not change the name, just password.   

To build an image run `docker build -t linkedsolutions:fuseki-server .`. Tag is optional

### Running
To run docker image `docker run -it --add-host=172.17.0.2:keycloak -p 9090:3030 smartswissparticipation:fuseki-server`
the Ip 172.17.0.2 should be replaced with actual ip of Keycloak container
you can access server at http://localhost:9090/

## Accessing
Any request to the fuseki server should contain the next header. 
(Except for admin user that can be authenticated with basic auth)
`Authorization: Bearer <keycloak access token value>`

## Security configuration
There is one predefined security graph: `<http://www.smartswissparticipation.com/security>` 
It will contain information about user access to the other graphs. 
This graph contains instances of `acl:Authorization` that grant a user or a class of users
access to a graph or all graph with a name matching a specified pattern.

Fuseki-OIDC uses the acl Ontology defined at http://www.w3.org/ns/auth/acl# offering partial support with extensions defined in https://linked.solutions/fuseki-oidc/ontology# described in [ontology.ttl](./ontology.ttl).

      
## Keycloak setup
* Get the last docker image jboss/keycloak
* Run with `docker run -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin -p 8080:8080 jboss/keycloak`   
  username and password can be changed off course 
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
