FROM openjdk:10 AS build
# Set up environment
RUN curl -o nodejs.tar.xz https://nodejs.org/dist/v12.14.1/node-v12.14.1-linux-x64.tar.xz
RUN mkdir /usr/local/bin/nodejs
RUN tar -xJvf nodejs.tar.xz -C /usr/local/bin/nodejs
ENV PATH=/usr/local/bin/nodejs/node-v12.14.1-linux-x64/bin:${PATH}
RUN npm install -g grunt grunt-cli bower
RUN curl -o /usr/local/bin/lein https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
RUN chmod a+x /usr/local/bin/lein

WORKDIR /opt/arvo-build
COPY aipal/ .
# Build frontend
WORKDIR /opt/arvo-build/frontend
RUN npm config set user 0
RUN npm config set unsafe-perm true
RUN npm install
RUN npm rebuild node-sass
RUN grunt build
# Build backend
WORKDIR /opt/arvo-build
RUN lein clean
RUN lein uberjar

#Build actual runtime container

FROM openjdk:10-jre-slim AS runtime
WORKDIR /opt/arvo

RUN groupadd -r arvo -g 1000 && useradd -u 1000 -r -g arvo -m -d /home/arvo -s /sbin/nologin -c "Arvo user" arvo 
RUN chmod 755 /home/arvo

USER arvo
COPY --from=build /opt/arvo-build/target/aipal-standalone.jar ./arvo.jar
COPY docker/logback.xml .

CMD ["java", "-jar", "arvo.jar"]
