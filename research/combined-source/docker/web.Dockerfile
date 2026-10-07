FROM node:24-bookworm-slim@sha256:d6aa754f16b3197301076f047b5def2f02ea1dbbc2ca920407d46d7ec7f87b20 AS node
FROM python:3.13.7-slim-bookworm@sha256:781449467ffb6f04218f09b1ecdcdc7d22b289ee5da9ec498b024e24ad7a6db7 AS build
COPY --from=node /usr/local/bin/node /usr/local/bin/node
WORKDIR /src
COPY . .
RUN python3 scripts/build-web.py && python3 scripts/qualify-web.py
FROM busybox:1.36@sha256:73aaf090f3d85aa34ee199857f03fa3a95c8ede2ffd4cc2cdb5b94e566b11662
COPY --from=build /src/web/ /site/
COPY --from=build /src/docs/ /site/docs/
COPY --from=build /src/evidence/qualification/ /site/evidence/qualification/
COPY --from=build /src/LICENSE /src/SECURITY.md /site/
RUN chown -R 65534:65534 /site
USER 65534:65534
EXPOSE 8080
CMD ["httpd", "-f", "-p", "8080", "-h", "/site"]
