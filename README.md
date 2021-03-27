# What

This demo project shows how to pass PoJos around Apache Beam's DoFns. Furthermore, it shows how to run the pipeline on
Apache Flink running on a Kubernetes cluster.

In this project we're building a super advanced AI powered historian that receives one of the many genocide events
throughout human history and outputs the perpetrators of that genocide.

Why the grim topic? It's important to acknowledge that technology facilitates all of society's ambitions, from the good
to the worst. While real time data processing is extremely important and useful, it can also be used for mass
surveillance and the elimination of human rights. It wouldn't be the first time technology is used for the worst, see
[IBM and the Holocaust](https://en.wikipedia.org/wiki/IBM_and_the_Holocaust).

# Plan

1. We're going to read a file containing a list of genocides, one on each line.
1. Each genocide will go through 3 DoFns that will populate a `Genocide` PoJo.
    * The first DoFn will populate the event field.
    * The second DoFn will fill the perpetrators, and the active years. To make things interesting, the active years
      properties will he held in a protobuf message.
    * The third DoFn will just output the pojo to stdout.

# Prerequisites

* A running Kubernetes cluster. Minikube, Microk8s, Kubernetes on Docker Desktop for Windows, etc.
* Java 8 or 11. At the time of writing, Beam only supports these two java versions.
* A docker registry (can be a private one, [Docker Registry](https://docs.docker.com/registry/))

# How (using PoJos between DoFns)

* Important note from the Beam mailgroup: Serializable has several disadvantages (e.g. non-deterministic coding for key
  grouping, less efficient serialization),
  see  https://beam.apache.org/documentation/programming-guide/#inferring-schemas
* Beam can run its transforms on different nodes/processes. It must therefore serialize the inputs and outputs of DoFns.
* In order to do that, you can create
  a [Coder](https://beam.apache.org/releases/javadoc/2.0.0/org/apache/beam/sdk/coders/Coder.html)  for your PoJo, but
  that gets a bit complicated.
* The PoJo class can also be decorated with the `@DefaultCoder(AvroCoder.class)` attribute or set
  by `.setCoder(AvroCoder.of(PoJo.class)` on the PCollection. Sometimes, the pipeline still complains that the coder
  cannot be inferred while executing. If your PoJo has generic arguments it gets trickier.
* The easiest solution to be able to use the PoJo is to make in implement `java.io.Serializable`. This works as long as
  all the properties inside the PoJo are Serializable. In our case the `When` property is a Protobuf Message, that is
  serializable (SURPRISE!), so things just work.
* You can run this project locally by
  executing: `mvn compile exec:java -Dexec.mainClass=org.aihistorian.Pipeline -Dexec.args=--runner=FlinkRunner` in the
  project's root folder.

# Docker image, le fat jar way (maven-shade-plugin)

* First we need to create a docker image containing with Flink and our Beam fat jar (all dependencies included).
* We can build this image by starting from a blank java image such as OpenJDK docker image, then installing maven,
  copying the source code and executing the compilation.
* Or, we can start from the `flink:java11` image ([link](https://hub.docker.com/_/flink)) and copy a jar file we built
  locally. Note that if you use Java 8, you need to use the appropriate flink image.
* To build the fat jar with all the required Beam dependencies, we use the `maven-shade-plugin` (see pom.xml) file.
    * uncomment the `var filePath = "file:///opt/flink/usrlib/genocides.txt";` file in Pipeline.java and comment the
      previous one
    * `mvn -Pshaded clean compile package` in the root directory will create
      the `tutorial-beam-flink-kubernetes-pojo-bundled-1.0-SNAPSHOT.jar`
* Then in our Dockerfile:
    * We copy the fat
      jar `COPY target/tutorial-beam-flink-kubernetes-pojo-bundled-1.0-SNAPSHOT.jar $FLINK_HOME/usrlib/app.jar`
    * We also need to copy our txt file `COPY genocides.txt $FLINK_HOME/usrlib/genocides.txt`
    * We build the image `docker build -t localhost:5000/aihistorian .`
    * We push the image `docker push localhost:5000/aihistorian`

# Docker image, le slim jar way (jib-maven-plugin)

* We can use Google's Jib plugin to create a slim jar. I recommend you
  read [this article](https://phauer.com/2019/no-fat-jar-in-docker-image/) on why this is a good idea. There are a few
  caveats.
* Flink expects all the jar and dependencies to be in `/opt/flink/lib/`. Jib puts everything in `/app`. We can change
  this behavior using ` <appRoot>/opt/flink/lib/app</appRoot>`. Now our code and all the dependencies will be in the
  java classpath Flink starts with. Note that I did try the -C (--classpath) parameter to the Flink CLI, it didn't work
  for me.
* Jib normally does not package your code into a jar. Instead, it uses the -cp parameter to give java the entry point,
  eg: ` -cp, /opt/flink/lib/app/classpath/*:/opt/flink/lib/app/libs/*, org.aihistorian.Pipeline`. Flink expects a jar.
  Adding `<packaging>jar</packaging>` to our pom will make them both happy.
* `mvn -Pjib clean compile package` in the root directory will create the docker image and push it to your private
  registry.

# How (Beam with Flink on Kubernetes)

* Flink (1.12) has native integration with Kubernetes, see links below.
* Now we need to execute our code:
    * [Download Flink](https://www.apache.org/dyn/closer.lua/flink/flink-1.12.2/flink-1.12.2-bin-scala_2.12.tgz)
    * Extract the files `tar -zxf flink-1.12.2-bin-scala_2.11.tgz`
    * Go in the extracted folder `cd flink-1.12.2`
    * Run the application
        ```
        ./bin/flink run-application \
        -c org.aihistorian.Pipeline \
        --detached \
        --parallelism 1 \
        --target kubernetes-application \
        -Dkubernetes.cluster-id=aihistorian \
        -Dkubernetes.container.image=localhost:5000/aihistorian \
        -Dkubernetes.jobmanager.cpu=0.5 \
        -Dkubernetes.taskmanager.cpu=0.5 \
        -Dtaskmanager.numberOfTaskSlots=4 \
        -Dkubernetes.rest-service.exposed.type=NodePort \
        local:///opt/flink/lib/app/classpath/tutorial-beam-flink-kubernetes-pojo-1.0-SNAPSHOT.jar\
        --runner=FlinkRunner
      // the above parameters are for Flink, if you need to pass in params to your app, pass them after your jar line
        --some-param \
        --some-param2=some_value \
      etc
        ```
    * Notice the sleep 10 minutes at the end of the Pipeline main method. Because this is a bounded source, the
      kubernetes deployment and associated pods will be deleted once the job is completed and we won't see the output.
    * You can inspect the creation of the pods with `kubectrl get pods`
        ```
        kubectl get pods
        NAME                           READY   STATUS    RESTARTS   AGE
        aihistorian-7f58fd9554-6j74x   1/1     Running   0          53s
        aihistorian-taskmanager-1-1    1/1     Running   0          6s
        ```
    * As previously stated, each Beam transform might be run on a different Kubernetes pod. In my case, I found the
      expected output on the taskmanager pod:
      ```
      Rohingya genocide started in 2016 and is presently ongoing. Perpetrated by Myanmar's armed forces and police.
      Darfur genocide started in 2003 and is presently ongoing. Perpetrated by Khartoum government, Janjaweed, the
      Justice and Equality Movement and the Sudan Liberation Army. 
      The Holocaust started in 1941 and ended in 1945.
      Perpetrated by Nazi Germany. 
      Armenian genocide started in 1915 and ended in 1922. Perpetrated by Ottoman
      Empire/Turkey. 
      Cambodian genocide started in 1975 and ended in 1979. Perpetrated by Khmer Rouge. 
      East Timor genocide started in 1975 and ended in 1999. Perpetrated by Indonesian New Order government. 
      Uyghur genocide started in 2014 and is presently ongoing. Perpetrated by People's Republic of China / Chinese Communist Party.
      ```

# Links

* [How to natively deploy Flink on Kubernetes with High-Availability (HA)](https://flink.apache.org/2021/02/10/native-k8s-with-ha.html)
* [Flink Native Kubernetes reference](https://ci.apache.org/projects/flink/flink-docs-stable/deployment/resource-providers/native_kubernetes.html)
* [Flink Cli reference](https://ci.apache.org/projects/flink/flink-docs-stable/deployment/cli.html#selecting-deployment-targets)
* [Beam Flink docs](https://beam.apache.org/documentation/runners/flink/)
* [Integrate Flink with Kubernetes natively by Yang Wang](https://www.youtube.com/watch?v=pdFPr_VOWTU)

# Conclusion

We have successfully run a Beam application on Flink on Kubernetes. There are a few subtle things you need to take into
account before going to production. Namely:

* Flink provides various execution modes, we only deployed using the Application Mode.
* Fat jars might not be the best for reproducible builds. We have seen how to use Jib for our images.
* The docker image for this sample project is around 800MB, not too long ago, that was the size of an entire OS.
* The Kubernetes deployment is created using the Flink client (which has a kubectrol client embedded). The usual way of
  deploying apps to Kubernetes is either though manually written yaml files and kubectrl or using Helm charts. As such,
  I'm unsure how to do things like Persistent Volume Claims, working with secrets etc. I still have some reading to do.

Please note that what I have written here is nothing new, it's compiled information from the articles in the Links
section. Go read them!