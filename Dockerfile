FROM  openjdk:8u151-jre-alpine3.7 
RUN echo http://mirror.yandex.ru/mirrors/alpine/v3.7/main > /etc/apk/repositories; \
    echo http://mirror.yandex.ru/mirrors/alpine/v3.7/community >> /etc/apk/repositories

RUN apk update \
 && apk add --no-cache 
RUN apk update && apk add jq && apk add bash && apk add curl


RUN mkdir /realm
ADD realm /opt/realm
ADD docker-entrypoint.sh /docker-entrypoint.sh
RUN mkdir -p /.m2/conf
ADD settings.xml /.m2/conf/settings.xml
ARG m2_variable=/.m2
ENV M2_HOME=$m2_variable
WORKDIR /

EXPOSE 5701
EXPOSE 5702
EXPOSE 5703
EXPOSE 5704
EXPOSE 5705
EXPOSE 5706
EXPOSE 15701 
EXPOSE 15702

HEALTHCHECK --interval=10s --timeout=10s --retries=30 CMD curl -f / http://localhost:80/version || exit 1 

ENTRYPOINT [ "/docker-entrypoint.sh" ]

ADD target/rulesservice-fat.jar /service.jar
