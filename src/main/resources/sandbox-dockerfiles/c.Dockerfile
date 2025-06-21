FROM alpine:latest

# Install only necessary packages for C compilation
RUN apk add --no-cache gcc musl-dev
RUN apk add --no-cache coreutils

# Create a non-root user
RUN adduser -D -u 1002 sandbox

# Copy script to root directory and make it executable (as root)
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Switch to non-root user
USER sandbox

# Optional: set working directory for sandbox user
WORKDIR /home/sandbox

# Call the entrypoint script from /
ENTRYPOINT ["/entrypoint.sh"]