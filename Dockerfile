FROM choffmeister/scala:2.11

ENV NODE_VERSION="0.12.2"

WORKDIR /tmp
RUN \
  wget --quiet http://nodejs.org/dist/v$NODE_VERSION/node-v$NODE_VERSION-linux-x64.tar.gz && \
  tar xfz node-v$NODE_VERSION-linux-x64.tar.gz && \
  mv node-v$NODE_VERSION-linux-x64 /opt/node && \
  ln -s /opt/node/bin/node /usr/local/bin/node && \
  ln -s /opt/node/bin/npm /usr/local/bin/npm && \
  npm install -g gulp && \
  npm install -g bower

ADD . /coreci
WORKDIR /coreci

ENTRYPOINT ["/usr/bin/sbt"]
CMD ["test"]
