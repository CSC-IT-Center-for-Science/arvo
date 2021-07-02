# Build frontend
FROM node:14-alpine AS nodebuild

RUN npm install -g grunt grunt-cli bower
WORKDIR /opt/resources/
COPY aipal/frontend/ .
RUN npm config set user 0
RUN npm config set unsafe-perm true
RUN npm install
RUN npm rebuild node-sass
RUN grunt build


# Build backend
FROM clojure:openjdk-8-lein-slim-buster AS clojurebuild
# Set up environment
WORKDIR /opt/arvo-build
COPY aipal/ .
WORKDIR /opt/arvo-build
COPY --from=nodebuild /opt/resources/public/ /opt/arvo-build/resources/public/
RUN lein clean
RUN lein uberjar


#Build actual runtime container
FROM azul/zulu-openjdk-alpine:8-jre AS runtime
WORKDIR /opt/arvo

USER 1000
COPY --from=clojurebuild /opt/arvo-build/target/aipal-standalone.jar ./arvo.jar
COPY docker/logback.xml resources/

CMD ["java", "-jar", "arvo.jar"]
