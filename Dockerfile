FROM centos:7

MAINTAINER Eric Löffler <eloeffler@aservo.com>

ADD etc/ /app/etc/
ADD src/ /app/src/
ADD project/build.properties /app/project/
ADD project/plugins.sbt /app/project/
ADD build.sbt /app/
ADD start.sh /app/

RUN groupadd -r -g 1000 appuser && \
    useradd -r -g 1000 -u 1000 appuser && \
    mkdir /var/app && \
    chown -R appuser:appuser /app && \
    chown -R appuser:appuser /var/app && \
    chmod a+x /app/start.sh && \
    mkhomedir_helper appuser

RUN yum makecache && \
    yum -y update

RUN curl https://bintray.com/sbt/rpm/rpm | tee /etc/yum.repos.d/bintray-sbt-rpm.repo

RUN yum makecache && \
	yum -y install \
        deltarpm \
        yum-utils && \
	rm -rf /tmp/* /var/tmp/*

COPY ca_certs/ /usr/share/pki/ca-trust-source/anchors/

RUN yum makecache && \
    yum -y install \
        ca-certificates && \
    update-ca-trust && \
    rm -rf /tmp/* /var/tmp/*

RUN yum makecache && \
	yum -y install \
        java-1.8.0-openjdk-headless \
        java-1.8.0-openjdk-devel \
        sbt && \
	yum clean all && \
	rm -rf /tmp/* /var/tmp/*

ENV JAVA_OPTS "-Dfile.encoding=UTF-8"

ENV SBT_OPTS "--no-colors" \
    "-Dsbt.global.base=/var/app/sbt" \
    "-Dsbt.boot.directory=/var/app/sbt/boot" \
    "-Dsbt.ivy.home=/var/app/ivy2"

WORKDIR /app

USER appuser

RUN sbt compile

CMD ["/app/start.sh"]
