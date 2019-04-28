FROM node
EXPOSE 8080
WORKDIR /usr/src/app
COPY . /usr/src/app
RUN npm install
RUN npm run-script build
CMD npx http-server dist
