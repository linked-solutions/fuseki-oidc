# Fuseki OIDC Sample Client

A simple client-side SPARQL client that allows authentication to demonstrate [Fuseki OIDC](https://github.com/linked-solutions/fuseki-oidc).

## Building

    docker build -t linkedsolutions/fuseki-oidc-sample-client .

## Running

    docker run -p 8081:8081 linkedsolutions/fuseki-oidc-sample-client