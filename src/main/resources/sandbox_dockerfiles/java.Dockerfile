FROM alpine:latest

RUN apk add --no-cache openjdk11 coreutils

RUN adduser -D -u 1002 sandbox

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

USER sandbox
WORKDIR /home/sandbox
ENTRYPOINT ["/entrypoint.sh"]
