## To build application you will need:
* JDK 8 or higher installed 
* Maven 3.5 or higher installed
* Docker

### Building
Put firebase-config.json file into src/main/resources directory. You can get this file there: https://console.cloud.google.com/iam-admin/serviceaccounts
To build an app run `mvn clean package`
To dockerize an app run `docker build -t org:fuseki-server .` after application is built with maven

### Running
To run docker image `docker run -it --rm -p 8080:8080 org:fuseki-server`
you can access server at http://localhost:8080/fuseki

## Accessing
Any request to the fuseki server should contain the next header
`Authorization: Bearer <firebase token value>`

To check if everything works you can:
* put some data into fuseki with request
```http request
POST http://localhost:8080/fuseki/ds
Authorization: Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6ImIzMmIyNzViNDBhOWFjNGU1ZmQ0NTFhZTUxMDE4ZThlOTgxMmViNDYiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL3NlY3VyZXRva2VuLmdvb2dsZS5jb20vYW8yLWRldi1maXJlYmFzZSIsIm9yZ2FuaXphdGlvbiI6InJlZmFjdG9yZWQtc3ViIiwiZW1haWwiOiJhbzIudXNlcjFAZ21haWwuY29tIiwiYXVkIjoiYW8yLWRldi1maXJlYmFzZSIsImF1dGhfdGltZSI6MTU0NTkwMjE2NywidXNlcl9pZCI6IjU2NDA4Njg3ODI0MDc2ODAiLCJzdWIiOiI1NjQwODY4NzgyNDA3NjgwIiwiaWF0IjoxNTQ1OTAyMTY3LCJleHAiOjE1NDU5MDU3NjcsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnt9LCJzaWduX2luX3Byb3ZpZGVyIjoiY3VzdG9tIn19.fW3d-RLiYW89XY2vD5-hS2XNzP6BgQLOV3MutIyUFOabLGIxAjebMcGOywjuYf7S1lRyHqasX0fLtWbM54t3gn9M_3tImr_B3zg2GQrzOQklYv65fGk_V9jjWJ2b6qAKjPSc1Mjn4wz5gcEIfPD08DbwSlTDjFnQVRcP5uYvpttDnJdfcUCRW8-qrNvDGKFk5HIxiwIovw2WdSPchHFNfOZBpIMDS61P5avBI9H4XG6PnCYK_jkikAYIFLSkqD0BoiLjbEzmwyT4N7ZQkefVN6BaPBUfkEuCcy8GYsWQCyTf5CMNsQXhhtZGk1ILH2GPcAraYOcEeoG9MNx22OpIwg
Content-Type: application/sparql-update

PREFIX dc: <http://purl.org/dc/elements/1.1/>
INSERT DATA {
    <http://example/book3>
        dc:title    "A new book" ;
        dc:creator  "Some Writer" .
}
```
* get data with request
```http request
POST http://localhost:8080/fuseki/ds
Authorization: Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6ImIzMmIyNzViNDBhOWFjNGU1ZmQ0NTFhZTUxMDE4ZThlOTgxMmViNDYiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL3NlY3VyZXRva2VuLmdvb2dsZS5jb20vYW8yLWRldi1maXJlYmFzZSIsIm9yZ2FuaXphdGlvbiI6InJlZmFjdG9yZWQtc3ViIiwiZW1haWwiOiJhbzIudXNlcjFAZ21haWwuY29tIiwiYXVkIjoiYW8yLWRldi1maXJlYmFzZSIsImF1dGhfdGltZSI6MTU0NTkwMjE2NywidXNlcl9pZCI6IjU2NDA4Njg3ODI0MDc2ODAiLCJzdWIiOiI1NjQwODY4NzgyNDA3NjgwIiwiaWF0IjoxNTQ1OTAyMTY3LCJleHAiOjE1NDU5MDU3NjcsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnt9LCJzaWduX2luX3Byb3ZpZGVyIjoiY3VzdG9tIn19.fW3d-RLiYW89XY2vD5-hS2XNzP6BgQLOV3MutIyUFOabLGIxAjebMcGOywjuYf7S1lRyHqasX0fLtWbM54t3gn9M_3tImr_B3zg2GQrzOQklYv65fGk_V9jjWJ2b6qAKjPSc1Mjn4wz5gcEIfPD08DbwSlTDjFnQVRcP5uYvpttDnJdfcUCRW8-qrNvDGKFk5HIxiwIovw2WdSPchHFNfOZBpIMDS61P5avBI9H4XG6PnCYK_jkikAYIFLSkqD0BoiLjbEzmwyT4N7ZQkefVN6BaPBUfkEuCcy8GYsWQCyTf5CMNsQXhhtZGk1ILH2GPcAraYOcEeoG9MNx22OpIwg
Content-Type: application/sparql-query
Accept: application/ld+json

PREFIX dc: <http://purl.org/dc/elements/1.1/>
CONSTRUCT {
   ?p dc:title ?t ;
      dc:creator ?c .
}
WHERE {
    ?p dc:title ?t .
    ?p dc:creator ?c .
}
```