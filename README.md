## To build application you will need:
* Docker

### Building
Put appropriate configs into src/main/resources/keycloack.json.
Most probably you will need to change just top 3 of them. 
It's important to have `auth-server-url` that matches url that end users will see during login, 
as system checks issuer of the token to ensure that server is not changed.
Additional information about [keycloak](https://www.keycloak.org/).   

In conf/shiro.ini you can find section `[users]` that contains one admin user with password.
You will need to change the password to more secure. 
Only this one user will be able to login with Basic auth. 
Security graph is prepopulated with the data that allows access to every graph for this user 
so do not change the name, just password.   

To build an image run `docker build -t smartparticipation:fuseki-server .`. Tag is optional

### Running
To run docker image `docker run -it --rm -p 8080:8080 smartparticipation:fuseki-server`
you can access server at http://localhost:8080/

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
      
