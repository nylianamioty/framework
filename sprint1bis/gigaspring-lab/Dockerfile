################################
# Normal setup
################################

FROM tomcat:10.1-jdk17
RUN rm -rf /usr/local/tomcat/webapps/*
COPY target/gigaspring-lab.war /usr/local/tomcat/webapps/gigaspring-lab.war

################################
# In case we want the project to be at / 
# and not at /project-name/
################################

# FROM tomcat:10.1-jdk17
# RUN rm -rf /usr/local/tomcat/webapps/ROOT
# COPY target/gigaspring-lab.war /usr/local/tomcat/webapps/ROOT.war
