FROM bellsoft/liberica-openjdk-alpine-musl:21.0.9
LABEL maintainer="opensourcegroup@netcracker.com"
LABEL atp.service="atp-mia"

ENV HOME_EX=/mia
WORKDIR $HOME_EX

RUN echo "https://dl-cdn.alpinelinux.org/alpine/v3.23/community/" >/etc/apk/repositories && \
    echo "https://dl-cdn.alpinelinux.org/alpine/v3.23/main/" >>/etc/apk/repositories && \
    apk add --update --no-cache --no-check-certificate \
        bash=5.3.3-r1 \
        curl=8.17.0-r1 \
        font-dejavu=2.37-r6 \
        fontconfig=2.17.1-r0 \
        gcompat=1.1.0-r4 \
        gettext=0.24.1-r1 \
        git=2.52.0-r0 \
        htop=3.4.1-r1 \
        jq=1.8.1-r0 \
        libpng=1.6.54-r0 \
        libcrypto3=3.5.4-r0 \
        libssl3=3.5.4-r0 \
        net-tools=2.10-r3 \
        nss_wrapper=1.1.12-r1 \
        pcre2=10.47-r0 \
        procps-ng=4.0.5-r0 \
        sops=3.11.0-r2 \
        sysstat=12.7.8-r0 \
        tcpdump=4.99.5-r1 \
        wget=1.25.0-r2 \
        xz-libs=5.8.2-r0 \
        zip=3.0-r13 && \
    rm -rf /var/cache/apk/*

COPY deployments/install deployments/install
COPY deployments/atp-common-scripts deployments/atp-common-scripts
COPY build-context/atp-mia-distribution/target/ /tmp/

RUN mkdir -p dist/atp deployments/update && \
    cp -r deployments/install/* deployments/update/ && \
    find deployments -maxdepth 1 -regex '.*/\(install\|update\|atp-common-scripts\)$' -exec mv -t dist/atp {} +

RUN adduser -D -H -h /atp -s /bin/bash -u 1007 atp && \
    mkdir -p /etc/env /etc/alternatives /tmp/log/diagnostic /tmp/cert && \
    ln -s ${JAVA_HOME}/bin/java /etc/alternatives/java && \
    echo "${JAVA_HOME}/bin/java \$@" >/usr/bin/java && \
    chmod a+x /usr/bin/java

RUN unzip /tmp/*.zip -d $HOME_EX/ && \
    rm -rf $HOME_EX/lib/*-sources.jar && \
    cp -r dist/atp /atp/ && chmod -R 775 /atp/ && \
    chown -R atp:root $HOME_EX/ && \
    find $HOME_EX -type f -name '*.sh' -exec chmod a+x {} + && \
    find $HOME_EX -type d -exec chmod 777 {} \;

EXPOSE 8080 9000 8000

USER atp

CMD [ "./run.sh" ]
