# Updated files for demo
Video of the demo is described in [this video](https://www.youtube.com/watch?v=4jI5GM_AOFs)

contains:

* docker-compose file
* traefik.toml for traefik 2.1

You need to:

* have control over four subdomains:
 * auth.my-domain.org  (keycloak will be launched here)
 * triplestore.my-domain.org  (the jena-fuseki)
 * app1.my-domain.org  (the test application)
 * www.my-domain.org   (a small hello-world to test certificates)
* These domains should all point to the same IP-address, which is the external IP of the machine you will be cloning this repo into

Afterwards:
1. replace in the docker-compose my-domain.org for your actual domain
2. change the admin password for keycloak  (line 79)
3. ```docker-compose up```
