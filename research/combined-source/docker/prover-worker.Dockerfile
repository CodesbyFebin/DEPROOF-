FROM golang:1.26.4-bookworm@sha256:b305420a68d0f229d91eb3b3ed9e519fcf2cf5461da4bef997bf927e8c0bfd2b AS build
WORKDIR /src/prover-worker
COPY prover-worker/go.mod prover-worker/go.sum ./
RUN GOTOOLCHAIN=local go mod download
COPY prover-worker/ ./
RUN CGO_ENABLED=0 GOTOOLCHAIN=local go test ./... && CGO_ENABLED=0 GOTOOLCHAIN=local go build -trimpath -o /out/prove ./cmd/prove && CGO_ENABLED=0 GOTOOLCHAIN=local go build -trimpath -o /out/verify ./cmd/verify
FROM python:3.13.7-slim-bookworm@sha256:781449467ffb6f04218f09b1ecdcdc7d22b289ee5da9ec498b024e24ad7a6db7
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 HOME=/home/deproof
RUN useradd --create-home --uid 10001 deproof && mkdir -p /state /export && chown 10001:10001 /state /export
WORKDIR /opt/deproof
COPY tools/requirements.txt tools/requirements.txt
RUN pip install --no-cache-dir -r tools/requirements.txt
COPY --from=build /out/ prover-worker/build/
COPY tools/ tools/
COPY scripts/qualify-device.py scripts/qualify-device.py
COPY fixtures/ fixtures/
COPY contracts/ contracts/
COPY app/schemas/ app/schemas/
COPY app/src/main/resources/ app/src/main/resources/
COPY docker/prover-entrypoint.sh docker/prover-entrypoint.sh
RUN chown -R 10001:10001 /opt/deproof
USER 10001:10001
ENTRYPOINT ["bash","/opt/deproof/docker/prover-entrypoint.sh"]
CMD ["idle"]
