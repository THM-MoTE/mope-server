
FROM openmodelica/openmodelica:v1.12.0
RUN apt-get update && apt-get install -y openjdk-8-jre

ADD ./target/scala-2.12/mope-server-*.jar /opt
RUN mv /opt/mope-server-*.jar /opt/mope-server.jar

EXPOSE 3000
CMD java -jar /opt/mope-server.jar --interface='0.0.0.0' --port=3000
