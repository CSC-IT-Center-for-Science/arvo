# Build frontend
FROM node:14-alpine AS nodebuild

WORKDIR /resources/
COPY frontend/ .
RUN npm install -g grunt grunt-cli bower && \
npm config set user 0 && \
npm config set unsafe-perm true && \
npm install && \
npm rebuild node-sass
RUN grunt build


# Build backend
FROM clojure:openjdk-8-lein-slim-buster AS clojurebuild
# Set up environment
WORKDIR /arvo-build
COPY . .
COPY --from=nodebuild /resources/public/ /arvo-build/resources/public/
# uberjar does clean by default if auto-clean is not set false
RUN lein uberjar


#Build actual runtime container
FROM azul/zulu-openjdk-alpine:8-jre AS runtime
WORKDIR /arvo

COPY docker/logback.xml resources/
COPY --from=clojurebuild /arvo-build/target/aipal-standalone.jar ./arvo.jar
# For openshift environments
RUN chmod 0755 resources/logback.xml arvo.jar

CMD ["java", "-jar", "arvo.jar"]
