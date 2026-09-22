FROM maven:3.9-eclipse-temurin-8 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B clean package -DskipTests


FROM egovio/apline-jre:8u121

WORKDIR /opt/egov

# Create all required DSC directories
RUN mkdir -p \
        /opt/egov/DS/Log \
        /opt/egov/DS/Temp \
        /opt/egov/DS/Lic \
        /opt/egov/DS/TempFiles \
        /DS/TempFiles \
    && chmod -R 777 /opt/egov/DS \
    && chmod -R 777 /DS/TempFiles

# Copy license
COPY OdishaUrban.lic /opt/egov/DS/Lic/OdishaUrban.lic

# Copy JAR
COPY --from=build /app/target/*.jar /opt/egov/dsc-services.jar

# Copy startup script
COPY start.sh /usr/bin/start.sh

RUN chmod +x /usr/bin/start.sh

CMD ["/usr/bin/start.sh"]
