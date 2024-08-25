FROM azul/zulu-openjdk-alpine:11.0.20.1-11.66.19-arm64 AS base
RUN apk update && apk add maven
COPY <<EOF /settings/settings.xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <localRepository>./.m2</localRepository>
</settings>
EOF

FROM base AS deps
WORKDIR /deps
COPY pom.xml /deps/
COPY backend/pom.xml /deps/backend/pom.xml
COPY webp-io/pom.xml /deps/webp-io/pom.xml

RUN mvn dependency:go-offline -s /settings/settings.xml

FROM base AS build
WORKDIR /sources
COPY . /sources/
COPY --from=deps /deps/.m2 /sources/.m2

RUN mvn clean package -Dmaven.test.skip=true -s /settings/settings.xml

FROM azul/zulu-openjdk-alpine:11.0.20.1-11.66.19-arm64

RUN apk update  \
    && apk add ffmpeg python3 py3-pip libwebp-tools optipng  \
    && pip3 install 'lottie==0.6.4' --break-system-packages

# Create the application directory
RUN mkdir /app

COPY --from=build /sources/backend/target/service.jar /app

WORKDIR /app

CMD [ "java", "-jar", "/app/service.jar" ]