FROM ubuntu:22.04

# Avoid prompts from apt
ENV DEBIAN_FRONTEND=noninteractive

# Install basic packages
RUN apt-get update && apt-get install -y \
    xfce4 \
    xfce4-goodies \
    tigervnc-standalone-server \
    tigervnc-common \
    novnc \
    websockify \
    supervisor \
    net-tools \
    iputils-ping\
    wget \
    dbus-x11 \
    xdg-utils \
    gnupg \
    ca-certificates \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Install Firefox ESR (works without snap)
RUN wget -q https://packages.mozilla.org/apt/repo-signing-key.gpg -O- | tee /etc/apt/keyrings/packages.mozilla.org.asc > /dev/null && \
    echo "deb [signed-by=/etc/apt/keyrings/packages.mozilla.org.asc] https://packages.mozilla.org/apt mozilla main" | tee -a /etc/apt/sources.list.d/mozilla.list > /dev/null && \
    echo 'Package: *\nPin: origin packages.mozilla.org\nPin-Priority: 1000' | tee /etc/apt/preferences.d/mozilla > /dev/null && \
    apt-get update && \
    apt-get install -y firefox && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Find Firefox binary and set as default browser
RUN FIREFOX_BIN=$(which firefox || find /usr -name firefox -type f 2>/dev/null | head -n1 || find /opt -name firefox -type f 2>/dev/null | head -n1) && \
    echo "Firefox found at: $FIREFOX_BIN" && \
    update-alternatives --install /usr/bin/x-www-browser x-www-browser $FIREFOX_BIN 200 && \
    update-alternatives --set x-www-browser $FIREFOX_BIN && \
    update-alternatives --install /usr/bin/gnome-www-browser gnome-www-browser $FIREFOX_BIN 200 && \
    update-alternatives --set gnome-www-browser $FIREFOX_BIN

# Create XFCE default browser configuration
RUN mkdir -p ${HOME}/.config/xfce4 && \
    mkdir -p ${HOME}/.local/share/applications && \
    echo "[Default Applications]" > ${HOME}/.config/mimeapps.list && \
    echo "x-scheme-handler/http=firefox.desktop" >> ${HOME}/.config/mimeapps.list && \
    echo "x-scheme-handler/https=firefox.desktop" >> ${HOME}/.config/mimeapps.list && \
    echo "text/html=firefox.desktop" >> ${HOME}/.config/mimeapps.list && \
    echo "application/xhtml+xml=firefox.desktop" >> ${HOME}/.config/mimeapps.list

# Set up user
ENV USER=root
ENV HOME=/root
ENV DISPLAY=:1

# Create supervisor config directory
RUN mkdir -p /var/log/supervisor

# Copy supervisor config
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]