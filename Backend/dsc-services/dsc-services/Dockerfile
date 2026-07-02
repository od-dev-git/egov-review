FROM egovio/apline-jre:8u121

# INSTRUCTIONS ON HOW TO BUILD JAR:
# Move to the location where pom.xml is exist in project and build project using below command
# "mvn clean package"

RUN mkdir /DS

RUN mkdir /DS/Log

RUN mkdir /DS/Temp

RUN mkdir /DS/Lic

RUN chmod -R 777 /DS/Temp

COPY *.lic /DS/Lic

COPY /target/dsc-services-1.1.0-SNAPSHOT.jar /opt/egov/dsc-services.jar

COPY start.sh /usr/bin/start.sh

RUN chmod +x /usr/bin/start.sh

CMD ["/usr/bin/start.sh"]

# NOTE: the two 'RUN' commands can probably be combined inside of a single
# script (i.e. RUN build-and-install-app.sh) so that we can also clean up the
# extra files created during the `mvn package' command. that step inflates the
# resultant image by almost 1.0GB.
