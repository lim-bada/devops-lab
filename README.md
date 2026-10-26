# devops-lab

「DevOps 이해 및 활용」 강의자료의 실습 코드다. 슬라이드에 나온 코드는 모두 이 폴더의 파일에서 잘라 낸 것이다.

## 폴더와 슬라이드

| 폴더 | 장 | 내용 |
| --- | --- | --- |
| `app/` | 3~7장 공용 | Spring Boot 4.1.1 · Java 21 앱. `GET /api/hello`, `/actuator/health` |
| `ch00-k8s/` | 0장 | `check-env.sh` 설치 확인(읽기 전용, `--pull` 을 주면 test Pod 로 image 받기까지) |
| `ch02-git/` | 2장 | `practice.sh`(init → commit → branch → merge), `conflict.sh`, `.gitignore` 예시 |
| `ch03-jenkins/` | 3장 | `docker-compose.yaml`, docker CLI 를 넣은 `Dockerfile`, `Jenkinsfile.hello`, 앱 빌드용 `Jenkinsfile` |
| `ch04-actions/` | 4장 | workflow 예제 8개. `.github/workflows/` 로 복사해서 쓴다 |
| `ch05-tekton/` | 5장 | `01-hello` → `02-basic` → `03-ci`(clone → test → buildah push) → `04-triggers` |
| `ch06-argocd/` | 6장 | Kustomize manifest 와 Argo CD `Application` (공개 nginx image) |
| `ch07-capstone/` | 6·7장 | manifest repository 템플릿(`gwangju/manifest-repo-template` 과 같다)과 따라 하기 README |
| `.github/workflows/ci-cd.yml` | 7장 | 처음에는 없다. `./gwangju/use-harbor-workflow.sh <N> <ID>` 가 `gwangju/workflows/ci-cd.yml` 에 값을 채워 만든다 |
| `gwangju/` | 0·3~7장 | 광주캠퍼스(skala-gj) 판. Harbor push, 반 namespace 배포, 개인 ID 이름 규칙. `gwangju/README.md` |

## 처음 3분

```bash
cd app
./gradlew test                 # test 2개. JDK 21 이 있으면 따로 설치할 것 없다
./gradlew bootRun              # 다른 터미널에서 curl localhost:8080/api/hello
docker build -t devops-lab-app:local .
docker run --rm -p 8080:8080 -e APP_VERSION=local devops-lab-app:local
```

응답 예시

```json
{"message":"hello, devops","version":"local"}
```

`version` 에는 `APP_VERSION` 값이 찍힌다. CI 는 여기에 git sha 7자리를 넣으므로, 배포 후 이 값을 보면 어느 commit 이 떠 있는지 바로 알 수 있다.

## 장별로 어디까지 돌려 봤나

강의자료를 만들면서 실제로 실행해 확인한 범위다.

| 대상 | 확인한 방법 | 결과 |
| --- | --- | --- |
| `app` | `./gradlew test`, docker build·run, `curl /api/hello` | 통과 |
| `ch02-git` | 임시 폴더에서 스크립트 실행 | 슬라이드 출력과 같음 |
| `ch03-jenkins` | `docker compose config`, Jenkins 컨테이너에서 Pipeline 실행(2026-10-22, 광주 Harbor) | test → image → push 통과, main 이 아닐 때 Push 건너뜀 |
| `ch04-actions` | actionlint, 2026-10-22 GitHub 에서 `ci.yml`·`docker.yml` 실행 | 오류 0, 두 workflow 성공 |
| `ch05-tekton` | kind 클러스터(Tekton Pipelines v1.16.0)에서 `01-hello`, `02-basic`, `03-ci` 실행. 2026-10-22 v1.17.0 에서 `03-ci` 다시 실행 | 전부 Succeeded. 광주 Harbor `<ID>-devops-lab-app-tekton` 으로 push |
| `ch06-argocd` | kind 에 `kubectl apply -k` | Pod 2개 Running |
| `ch07-capstone`, `gwangju` | 2026-10-22 광주 클러스터에서 GitHub Actions → Harbor → Argo CD 끝까지 | 새 version 응답 확인. 결과는 `gwangju/README.md` |

GitHub Actions 는 임시 저장소를 만들어 실제 GitHub 에서 실행했다. 첫 실행에서 막히면 `ch04-actions/README.md` 와 `ch07-capstone/README.md` 의 문제 해결 표를 본다.

## 자주 막히는 곳

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| `./gradlew: Permission denied` (CI) | Windows 에서 commit 해서 실행 권한이 빠짐 | `git update-index --chmod=+x app/gradlew` 후 commit |
| buildah 에서 `short-name resolution enforced` | Dockerfile `FROM` 에 짧은 image 이름 | `docker.io/library/...` 처럼 전체 이름을 쓴다(`app/Dockerfile` 참고) |
| kubectl 명령이 EKS 로 간다 | current-context 가 EKS | `kubectl config use-context docker-desktop` |
| Harbor login `unauthorized` | `HARBOR_USERNAME`·`HARBOR_PASSWORD` 값 | `gh secret set` 으로 다시 등록 |
| Argo CD `ImagePullBackOff` | 첫 CI 전 tag `0000000`, 또는 `imagePullSecrets` 누락 | Actions 가 끝난 뒤 등록, deployment 의 `harbor-cred` 확인 |

## 운영으로 가져가기 전에 바꿀 것

- [ ] `ch03-jenkins/docker-compose.yaml` 의 `user: root` 와 docker.sock 마운트를 없애고 build 는 별도 agent 로
- [ ] PAT 는 classic 대신 fine-grained, 대상 repository 하나로 제한
- [ ] Tekton buildah Task 는 privileged 로 돈다. 전용 namespace·노드로 분리
- [ ] image tag 는 git sha 로 고정하고 `latest` 는 배포에 쓰지 않는다
- [ ] 반 공용 Harbor 계정 대신 프로젝트별 robot 계정을 사용한다
- [ ] Argo CD `admin` 초기 비밀번호 변경, `argocd-initial-admin-secret` 삭제
