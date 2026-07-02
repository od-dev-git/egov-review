FROM egovio/alpine-maven-builder-jdk-8:gcp AS build

# Create runtime image
#USER root
#RUN chmod -R 777 /root
RUN mkdir /opt/jboss/
COPY wildfly.zip /opt/jboss/
RUN cd /opt/jboss/ && unzip wildfly.zip && rm wildfly.zip
#RUN chmod -R 777 /opt/jboss/
COPY *.war /opt/jboss/wildfly/standalone/deployments/
COPY ds.zip /opt/jboss/wildfly/standalone/
RUN cd /opt/jboss/wildfly/standalone/ && unzip ds.zip && rm ds.zip
RUN chmod -R 777 /opt/jboss
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
#CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0","-Ddb.migration.enabled=false", "-Ddev.mode=true", "-#Ddb.flyway.validateon.migrate=true"]

