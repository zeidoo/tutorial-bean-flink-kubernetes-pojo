FROM flink:java11
RUN mkdir -p $FLINK_HOME/usrlib
COPY target/tutorial-beam-flink-kubernetes-pojo-bundled-1.0-SNAPSHOT.jar $FLINK_HOME/usrlib/app.jar
COPY genocides.txt $FLINK_HOME/usrlib/genocides.txt