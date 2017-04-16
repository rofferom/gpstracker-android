FROM debian:stretch
MAINTAINER Romain Roffé <rofferom@gmail.com>

RUN apt-get update && apt-get install -y openjdk-8-jdk git-core wget zip

ENV ANDROID_HOME /opt/android_sdk

RUN mkdir -p ${ANDROID_HOME}
WORKDIR ${ANDROID_HOME}

RUN wget https://dl.google.com/android/repository/tools_r25.2.3-linux.zip
RUN apt-get install -y zip
RUN unzip tools_r25.2.3-linux.zip
RUN rm tools_r25.2.3-linux.zip

RUN (while sleep 3; do echo "y"; done) | ./tools/android update sdk -u -a --filter platform-tools,build-tools-25.0.2,android-25,extra-android-m2repository

COPY checkoutAndBuild.sh /
WORKDIR /output
CMD /checkoutAndBuild.sh

