FROM java:8
VOLUME /tmp
ADD ccweb-start-0.0.1-SNAPSHOT.jar ccweb-start.jar
RUN sh -c 'touch /ccweb-start.jar'
ENV JAVA_OPTS=""
ENV isuat=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Xms255m -Xmx255m -Xss255k -Duser.timezone=GMT+08 -Dplan=true -Disuat=$isuat -jar /ccweb-start.jar" ]