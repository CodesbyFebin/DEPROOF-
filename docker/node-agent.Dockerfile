FROM golang:1.26.4-bookworm@sha256:b305420a68d0f229d91eb3b3ed9e519fcf2cf5461da4bef997bf927e8c0bfd2b AS build
WORKDIR /src/prover-worker
COPY prover-worker/go.mod prover-worker/go.sum ./
RUN GOTOOLCHAIN=local go mod download
COPY prover-worker/ ./
RUN CGO_ENABLED=0 GOTOOLCHAIN=local go test ./... && CGO_ENABLED=0 GOTOOLCHAIN=local go build -trimpath -o /out/prove ./cmd/prove && CGO_ENABLED=0 GOTOOLCHAIN=local go build -trimpath -o /out/verify ./cmd/verify
WORKDIR /src/node-agent
COPY node-agent/ ./
RUN CGO_ENABLED=0 GOTOOLCHAIN=local go test ./... && CGO_ENABLED=0 GOTOOLCHAIN=local go build -trimpath -o /out/deproof-node ./cmd/deproof-node
FROM python:3.13.7-slim-bookworm@sha256:781449467ffb6f04218f09b1ecdcdc7d22b289ee5da9ec498b024e24ad7a6db7 AS runtime
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 HOME=/home/deproof
RUN useradd --create-home --uid 10001 deproof && mkdir -p /state /export && chown 10001:10001 /state /export
WORKDIR /opt/deproof
COPY tools/requirements.txt tools/requirements.txt
RUN pip install --no-cache-dir -r tools/requirements.txt
COPY --from=build /out/ prover-worker/build/
COPY tools/ tools/
COPY fixtures/ fixtures/
COPY contracts/ contracts/
COPY app/schemas/ app/schemas/
COPY app/src/main/resources/ app/src/main/resources/

COPY docker/node-entrypoint.sh docker/node-entrypoint.sh
RUN chown -R 10001:10001 /opt/deproof
USER 10001:10001
ENTRYPOINT ["bash","/opt/deproof/docker/node-entrypoint.sh"]

FROM docker:29.5.2-cli@sha256:9ba8e32bfc35a2c7ae2feb1e3241b2778ae21dee80f4dcd31d04e1cfdea86ea2 AS docker-cli
FROM runtime AS owner-hosting
USER 0:0
COPY --chown=0:0 evidence/qualification/hosting-isolation.json /owner/hosting-isolation.json
RUN chown -R 0:0 /opt/deproof /state
COPY --from=docker-cli /usr/local/bin/docker /usr/local/bin/docker
