FROM ubuntu:latest

MAINTAINER Eric Löffler <eloeffler@aservo.com>

ADD ca_certs/ /usr/local/share/ca-certificates/
ADD target/dist/ /app/lib/
ADD init.sh /app/
ADD start.sh /app/

ENV DEBIAN_FRONTEND=noninteractive

RUN chmod -R 644 /usr/local/share/ca-certificates/ && \
    groupadd -r -g 1000 appuser && \
    useradd -r -g 1000 -u 1000 appuser && \
    chown -R appuser:appuser /app

RUN \
    apt-get update && \
    apt-get --no-install-recommends --yes install \
        ca-certificates \
        openssl \
        ncat \
        jq \
        openjdk-11-jdk-headless \
        && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN update-ca-certificates

WORKDIR /app

USER appuser

ENTRYPOINT ["/app/init.sh"]

CMD ["/app/start.sh"]
