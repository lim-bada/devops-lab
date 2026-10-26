# 3장 Jenkins 실습

| 파일 | 내용 |
| --- | --- |
| `docker-compose.yaml` | Jenkins controller 1대. `jenkins_home` named volume, docker.sock 마운트 |
| `Dockerfile` | 공식 `jenkins/jenkins:lts-jdk21` 에 docker CLI 와 buildx 만 추가 |
| `Jenkinsfile.hello` | 첫 Pipeline. echo 만 하는 Build/Test/Deploy |
| `Jenkinsfile` | 앱 빌드 Pipeline. `app/` 의 gradle test → docker build → (main 이면) push |

## 실행

```bash
docker compose up -d --build
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
open http://localhost:8080          # Install suggested plugins → 관리자 계정 생성
```

## 앱 빌드 Pipeline 연결

1. 새로운 Item → Pipeline
2. Definition: **Pipeline script from SCM**, SCM: Git, Repository URL: 이 저장소
3. Branch Specifier: `*/main`, **Script Path: `ch03-jenkins/Jenkinsfile`**
4. push 까지 하려면 Jenkins 관리 → Credentials 에 `harbor-login` (Username with password,
   username 은 반 계정 `skala-gj<N>`, password 는 반 Harbor 비밀번호) 을 등록하고,
   Jenkinsfile 의 `IMAGE` 에 반 번호 `<N>` 과 개인 ID `<ID>` 를 넣는다

이 Jenkinsfile 은 Jenkins 컨테이너에서 실제로 돌려서 Test(junit 기록) → Image 까지 통과하고,
main 이 아닌 branch 에서 Push 가 건너뛰어지는 것을 확인했다.

## 주의

- `user: root` 와 docker.sock 마운트는 **실습용**이다. 이 조합이면 Jenkins 안의 모든 job 이
  호스트 Docker 를 root 로 다룰 수 있다. 운영에서는 별도 agent(또는 Kubernetes agent)에서 build 한다.
- `docker compose down -v` 는 `jenkins_home` volume 까지 지운다. 설정과 job 이 모두 사라진다.
- ngrok 무료 주소는 재시작할 때마다 바뀐다. 바뀌면 GitHub webhook 의 Payload URL 도 고친다.
