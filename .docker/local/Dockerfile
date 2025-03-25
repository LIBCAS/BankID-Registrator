FROM eclipse-temurin:11

RUN apt-get update && apt-get -y upgrade && \
    apt-get install -y dos2unix \
    libglib2.0-0 \
    libnss3 \
    libfontconfig1 \
    libx11-xcb1 \
    libxcomposite1 \
    libxcursor1 \
    libxdamage1 \
    libxi6 \
    libxtst6 \
    libxrandr2 \
    libatk1.0-0 \
    libgtk-3-0 \
    libgbm1 \
    wget gnupg2 && \
    wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - && \
    sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list' && \
    apt-get update && \
    apt-get install -y google-chrome-stable && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

ENV HOME=/app
RUN mkdir -p $HOME
WORKDIR $HOME