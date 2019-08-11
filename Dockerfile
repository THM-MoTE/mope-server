
FROM openmodelica/openmodelica:v1.12.0

ADD https://download.java.net/java/GA/jdk12.0.2/e482c34c86bd4bf8b56c0b35558996b9/10/GPL/openjdk-12.0.2_linux-x64_bin.tar.gz /tmp
RUN tar xzf /tmp/openjdk-12.0.2_linux-x64_bin.tar.gz --directory /opt &&\
  /opt/jdk-12.0.2/bin/java -version &&\
  ln -s /opt/jdk-12.0.2/bin/java /usr/bin/java

RUN useradd -ms /bin/bash openmodelica
RUN mkdir -p /home/openmodelica/data &&\
  mkdir -p /home/openmodelica/.config/mope &&\
  chown -R openmodelica:openmodelica /home/openmodelica

ADD ./target/scala-2.12/mope-server-*.jar /opt
RUN mv /opt/mope-server-*.jar /opt/mope-server.jar &&\
  chmod 777 /opt/mope-server.jar

USER openmodelica

EXPOSE 3000
CMD /usr/bin/java -jar /opt/mope-server.jar --interface='0.0.0.0' --port=3000
