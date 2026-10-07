FROM eclipse-temurin:17.0.20_8-jdk-noble@sha256:9f2d47abfd5e58b6c31c110043c6f5717beccd912b724988e3e0166266b8e570
ARG BUILDER_UID=10001
ARG BUILDER_GID=10001
ARG SDK_TOOLS=15859902
ARG SDK_TOOLS_SHA256=4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583
ENV ANDROID_HOME=/opt/android-sdk ANDROID_SDK_ROOT=/opt/android-sdk GRADLE_USER_HOME=/cache/gradle HOME=/home/builder
RUN apt-get update && apt-get install -y --no-install-recommends curl unzip ca-certificates python3 && rm -rf /var/lib/apt/lists/* \
 && mkdir -p /opt/android-sdk/cmdline-tools \
 && curl --fail --location --retry 3 "https://dl.google.com/android/repository/commandlinetools-linux-${SDK_TOOLS}_latest.zip" -o /tmp/sdk.zip \
 && echo "${SDK_TOOLS_SHA256}  /tmp/sdk.zip" | sha256sum -c - \
 && unzip -q /tmp/sdk.zip -d /tmp/sdk \
 && mv /tmp/sdk/cmdline-tools /opt/android-sdk/cmdline-tools/pinned \
 && rm -rf /tmp/sdk /tmp/sdk.zip \
 && yes | /opt/android-sdk/cmdline-tools/pinned/bin/sdkmanager --sdk_root=/opt/android-sdk --licenses >/dev/null

RUN /opt/android-sdk/cmdline-tools/pinned/bin/sdkmanager --sdk_root=/opt/android-sdk 'platforms;android-37.0' 'build-tools;36.0.0' \
 && (getent group "${BUILDER_GID}" || groupadd --gid "${BUILDER_GID}" builder) \
 && useradd --create-home --uid "${BUILDER_UID}" --gid "${BUILDER_GID}" builder \
 && mkdir -p /workspace /cache/gradle /export \
 && chown -R "${BUILDER_UID}:${BUILDER_GID}" /workspace /cache /export /home/builder
WORKDIR /source
COPY . .
RUN test ! -e local.properties && chown -R "${BUILDER_UID}:${BUILDER_GID}" /source
USER ${BUILDER_UID}:${BUILDER_GID}
ENTRYPOINT ["bash", "/source/docker/android-entrypoint.sh"]
