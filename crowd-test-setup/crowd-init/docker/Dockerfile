FROM ubuntu:latest

MAINTAINER Eric Löffler <eloeffler@aservo.com>

ARG UID=1000
ARG GID=1000

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
    apt-get -y upgrade && \
    apt-get -y --no-install-recommends install \
        ca-certificates \
        ncat \
        curl \
        jq \
        && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

ADD docker-entrypoint.sh /opt/app/
ADD docker-start.sh /opt/app/
ADD crowd-init.sh /usr/local/sbin/

RUN groupadd -r -g "$GID" appuser && \
    useradd -r -g "$GID" -u "$UID" appuser && \
    chown -R appuser:appuser /opt/app

USER appuser

WORKDIR /opt/app

ENTRYPOINT ["/opt/app/docker-entrypoint.sh"]

CMD ["/opt/app/docker-start.sh"]
