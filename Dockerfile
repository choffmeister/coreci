FROM choffmeister/scala

RUN \
  add-apt-repository --yes ppa:chris-lea/node.js && \
  apt-get update && \
  apt-get install --yes nodejs && \
  npm install -g gulp && \
  npm install -g bower

ADD . /coreci

WORKDIR /coreci

RUN sbt test
