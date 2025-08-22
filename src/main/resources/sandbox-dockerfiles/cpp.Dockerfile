FROM alpine:latest

RUN apk add --no-cache g++ musl-dev
RUN apk add --no-cache coreutils

RUN adduser -D -u 1002 sandbox

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

USER sandbox
WORKDIR /home/sandbox
ENTRYPOINT ["/entrypoint.sh"]
