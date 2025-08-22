FROM alpine:latest

RUN apk add --no-cache rust cargo coreutils

RUN adduser -D -u 1002 sandbox

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

USER sandbox
WORKDIR /home/sandbox
ENTRYPOINT ["/entrypoint.sh"]
