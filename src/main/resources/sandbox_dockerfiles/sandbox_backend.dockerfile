FROM docker:27-dind

# Install docker-compose plugin (already included in modern Docker versions, but ensuring)
RUN apk add --no-cache docker-cli-compose bash

# Create workspace
WORKDIR /app

# Copy your existing compose file into the image
COPY docker-compose.yml /app/docker-compose.yml

# Default command: start Docker daemon and run the compose setup
CMD ["sh", "-c", "dockerd-entrypoint.sh & sleep 5 && docker compose up --build"]
