# 공식 Jenkins 이미지에는 docker 명령이 없다.
# docker.sock 만 마운트하면 "docker: not found" 가 나므로 CLI 만 복사해 넣는다.
FROM jenkins/jenkins:lts-jdk21
USER root
COPY --from=docker:cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=docker:cli /usr/local/libexec/docker/cli-plugins/docker-buildx \
     /usr/local/libexec/docker/cli-plugins/docker-buildx
