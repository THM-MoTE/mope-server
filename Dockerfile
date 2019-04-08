
FROM openmodelica/openmodelica:v1.12.0

RUN wget https://download.java.net/java/GA/jdk11/9/GPL/openjdk-11.0.2_linux-x64_bin.tar.gz -O /tmp/openjdk.tar.gz
RUN tar xfvz /tmp/openjdk.tar.gz --directory /opt &&\
  /opt/jdk-11.0.2/bin/java -version

RUN useradd -ms /bin/bash openmodelica
RUN mkdir -p /home/openmodelica/data &&\
  mkdir -p /home/openmodelica/.config/mope &&\
  chown -R openmodelica:openmodelica /home/openmodelica

ADD ./target/scala-2.12/mope-server-*.jar /opt
RUN mv /opt/mope-server-*.jar /opt/mope-server.jar &&\
  chmod 777 /opt/mope-server.jar

USER openmodelica

EXPOSE 3000
CMD /opt/jdk-11.0.2/bin/java -jar /opt/mope-server.jar --interface='0.0.0.0' --port=3000
