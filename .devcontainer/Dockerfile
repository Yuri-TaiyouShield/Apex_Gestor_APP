FROM mcr.microsoft.com/devcontainers/base:ubuntu-24.04

ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
  && apt-get install -y --no-install-recommends \
    bash-completion \
    ca-certificates \
    curl \
    git \
    jq \
    lsof \
    mysql-client \
    netcat-openbsd \
    openssh-client \
    postgresql-client \
    procps \
    unzip \
    zip \
  && rm -rf /var/lib/apt/lists/*

ENV APEX_CDE=true \
    TZ=America/Sao_Paulo
