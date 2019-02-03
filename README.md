## To build application you will need:
* Docker

### Building
Put appropriate configs into src/main/resources/keycloack.json.
Most probably you will need to change just top 3 of them. 
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

To build an image run `docker build -t smartparticipation:fuseki-server .`. Tag is optional

### Running
To run docker image `docker run -it --add-host=172.17.0.2:docker.server.com -p 9090:3030 smartparticipation:fuseki-server`
the Ip 172.17.0.2 should be replaced with actual ip of Keycloak container
you can access server at http://localhost:9090/

## Accessing
Any request to the fuseki server should contain the next header. 
(Except for admin user that can be authenticated with basic auth)
`Authorization: Bearer <keycloak access token value>`

## Security configuration
There is one predefined security graph: `<http://www.smartparticipation.com/security>` 
It will contain information about user access to the other graphs. 
There are just two classes:
* User that have properties:
   * `<http://www.smartparticipation.com/users#email>` as email
   (for admin it's not email actually to distinguish it from other users)
   * `<http://www.smartparticipation.com/security#graphAccess>` Uri of GraphAccess instance
   There can be arbitrary count of graphAccess properties for a user
* GraphAccess that have properties
   * `<http://www.smartparticipation.com/graphs#graph>` Graph name. String literal that can use Ant wildcards. (see details [here](http://ant.apache.org/manual/dirtasks.html#patterns))
   * `<http://www.smartparticipation.com/graphs#accessType>` String literal that can take next values : 
      * READ
      * WRITE (is a group of the next ones)
      * CREATE
      * DELETE
      * UPDATE
      
## Keycloak setup
* Get the last docker image jboss/keycloak
* Run with `docker run -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin -p 8080:8080 jboss/keycloak`   
  username and password can be changed off course 
* Go to localhost:6060/auth, login admin with credentials
* Open "Realm Settings" -> "Tokens" and setup default signature to RS-256
* Setup own users login/signup in "Realm Settings" -> "Login" 
  or just add any identity provider in "Identity Providers" page   
  _IMPORTANT_ you need to include `email` into "Default Scopes" of an identity provider
* setup new client for frontend, set name and url. 
  After it created add an email mapper in "Mappers" tab using "Add Builtin" button
* Library for frontend interaction with Keycloak can be found [here](https://www.npmjs.com/package/keycloak-js)

Additional information about [keycloak](https://www.keycloak.org/).  