# 3장 Jenkins 실습 진행 가이드

교안: `DevOps 이해 및 활용(광주)_2610.pdf`, PDF 뷰어 기준 **62~108쪽**.
교안 화면 아래에 인쇄된 쪽수는 PDF 뷰어보다 1 작다. 예를 들어 PDF 66쪽은 인쇄된 65쪽이다.

목표는 **간단한 Pipeline 실행 → GitHub의 Pipeline 파일 읽기 → push 시 자동 실행 → 앱 테스트·이미지 빌드·Harbor 업로드**다.
3장 완료 기준은 Harbor에 내 이미지의 commit SHA 태그가 생기는 것이다. 클러스터 배포는 6~7장에서 이어 간다.

## 0. 현재 상태와 사용할 파일

2026-10-26에 확인한 상태:

- `jenkins` 컨테이너가 `devops-lab/jenkins:lts-jdk21` 이미지로 실행 중이다.
- 맥의 8080 포트는 이 Jenkins에 연결되어 있다.
- Jenkins 안의 Docker CLI와 호스트 Docker 연결도 정상이다.
- 이 실습 폴더 최상위에는 `.git`이 없다. GitHub에서 코드를 읽는 단계 전에 저장소를 준비해야 한다.
- 본인 설정은 **3반, 개인 ID `g094`**로 확인했다. `ch03-jenkins/Jenkinsfile`의 현재 이미지 주소 `harbor.skala-gj.com/skala-gj3/g094-devops-lab-app`이 본인 설정과 일치하므로 수정할 필요가 없다.
- GitHub 실습 저장소는 `https://github.com/lim-bada/devops-lab.git`이다. 원격 HEAD 조회에서 아직 commit이 확인되지 않아, 3단계에서 로컬 코드를 처음 업로드한다.

| 위치 | 역할 | 사용하는 때 |
| --- | --- | --- |
| [docker-compose.yaml](docker-compose.yaml) | Jenkins 실행 설정 | Jenkins 시작·종료 |
| [Dockerfile](Dockerfile) | Docker 명령이 들어 있는 Jenkins 이미지 제작 | `up --build` |
| [Jenkinsfile.hello](Jenkinsfile.hello) | Build → Test → Deploy에서 메시지만 출력 | 첫 Pipeline 연습 |
| [Jenkinsfile](Jenkinsfile) | 실제 앱 테스트·이미지 빌드·Harbor 업로드 | 후반 실습 |
| [../app/](../app/) | 빌드할 Spring Boot 앱 | 실제 앱 Pipeline |

`ch02-git/git-lab`이나 `conflict-lab`은 Git 명령 연습용이다. 이번 SCM 연동에는 GitHub 저장소 최상위에 `app/`, `ch03-jenkins/`가 있는 저장소를 사용한다.

## 1. Jenkins 접속과 초기 설정 — PDF 67~75쪽

맥 터미널에서 실행한다.

```bash
cd /Users/limhaean/workspace/16-devops-lab/ch03-jenkins
docker compose ps
open http://localhost:8080
```

현재는 Jenkins가 이미 실행 중이므로 브라우저 접속부터 하면 된다. 컨테이너가 멈춰 있거나 처음 만드는 경우에는 아래 명령을 사용한다.

```bash
docker compose up -d --build
```

- `-d`: 터미널을 계속 점유하지 않고 백그라운드에서 실행한다.
- `--build`: 이 폴더의 Dockerfile로 Jenkins 이미지를 빌드한다. 후반의 `docker build` 실습에 필요하다.

브라우저 화면에 따라 진행한다.

| 보이는 화면 | 할 일 |
| --- | --- |
| Unlock Jenkins | 아래 명령으로 확인한 초기 비밀번호 입력 |
| 플러그인 설치 선택 | `Install suggested plugins` 선택 |
| 관리자 계정 생성 | 사용할 관리자 계정 생성 후 안내대로 완료 |
| 로그인 화면 | 앞서 만든 관리자 계정으로 로그인 |
| 대시보드 | 2단계로 이동 |

초기 잠금 화면에서만 필요한 명령:

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

필요한 플러그인은 Pipeline, Git, GitHub, Credentials Binding, JUnit, Pipeline: Stage View다. 메뉴가 없거나 단계별 화면이 보이지 않으면 `Jenkins 관리 → Plugins`에서 설치 여부를 확인한다.

**성공 기준:** Jenkins 대시보드에 로그인할 수 있다.

## 2. 화면에 입력하는 첫 Pipeline — PDF 76~83쪽

1. 대시보드에서 `새로운 Item / New Item`을 클릭한다.
2. 이름을 `hello-pipeline`으로 정하고 종류는 **Pipeline**을 선택한다.
3. 설정 화면 아래의 Pipeline에서 Definition을 **Pipeline script**로 선택한다.
4. [Jenkinsfile.hello](Jenkinsfile.hello)의 전체 내용을 Script 칸에 붙여 넣는다.
5. 저장하고 **지금 빌드 / Build Now**를 클릭한다.
6. 생성된 빌드 번호를 클릭하고 **Console Output**을 연다.

macOS에서 파일 내용을 복사하려면, `ch03-jenkins` 폴더에서 다음 명령을 사용할 수 있다.

```bash
pbcopy < Jenkinsfile.hello
```

**성공 기준:** Build → Test → Deploy가 성공하고 Console Output 마지막에 `Finished: SUCCESS`가 나온다.
이 단계의 Build·Test·Deploy는 이름과 메시지를 익히는 연습이며, 실제 앱을 빌드하거나 배포하지는 않는다.

교안 PDF 81쪽의 `checkout scm`은 SCM 연결이 필요한 코드다. 첫 화면 입력 연습에서는 샘플 `Jenkinsfile.hello`를 사용한다. SCM 연동은 다음 단계에서 설정한다.

## 3. GitHub 실습 저장소 준비

Jenkins는 다음 단계부터 GitHub에 올라간 파일을 읽는다. 로컬 파일을 수정했으면 commit과 push까지 해야 Jenkins에 반영된다.

GitHub 저장소 첫 화면에 다음 구조가 보여야 한다.

```text
저장소 최상위
├── app/
│   ├── gradlew
│   ├── build.gradle
│   └── src/
└── ch03-jenkins/
    ├── Jenkinsfile.hello
    └── Jenkinsfile
```

이미 본인 GitHub에 이 구조의 저장소가 있다면 그 URL을 사용하고, 수정할 때는 해당 저장소를 clone한 로컬 폴더에서 작업한다.

이번 실습의 GitHub 저장소는 [lim-bada/devops-lab](https://github.com/lim-bada/devops-lab)이다. 원격 HEAD 조회에서 아직 commit이 확인되지 않았고, 로컬 폴더에도 `.git`이 없으므로 다음 명령으로 첫 업로드를 진행한다.

```bash
cd /Users/limhaean/workspace/16-devops-lab
git init -b main
git add README.md app ch03-jenkins
git status
git commit -m "chore: add app and Jenkins lab"
git remote add origin https://github.com/lim-bada/devops-lab.git
git push -u origin main
```

위 명령은 새 빈 저장소용이다. 원격 저장소에 이미 이력이 있으면 그 저장소를 clone해서 사용한다.
`git add`는 3장에 필요한 경로만 지정했다. 다른 장은 실습할 때 추가하면 된다.
HTTPS push 인증이 필요하면 2장에서 준비한 인증을 사용한다. 아래 Jenkins용 읽기 토큰과 로컬 push에 필요한 쓰기 권한은 용도가 다르다.

**성공 기준:** GitHub 웹 화면에서 `app/`과 `ch03-jenkins/`의 파일을 열 수 있다.

## 4. GitHub 자격 증명 등록 — PDF 87~95쪽

교안의 PAT는 Jenkins가 GitHub 저장소를 읽을 때 사용하는 토큰이다. Harbor 비밀번호와는 별개다.

GitHub 개인 설정 → Developer settings → Personal access tokens에서 준비한다. 기존에 만든 적절한 토큰이 있으면 재사용할 수 있다.
새 fine-grained token을 만든다면 본인 실습 저장소를 선택하고 **Contents: Read-only**로 설정한다.
이번 가이드처럼 GitHub 웹 화면에서 webhook을 직접 등록하면 Jenkins의 읽기 토큰에 Webhooks 쓰기 권한을 추가할 필요는 없다. 교안의 webhook 쓰기 권한은 API로 webhook을 관리할 때 쓰는 권한이다.

Jenkins의 `Jenkins 관리 → Credentials → System → Global credentials → Add Credentials`에서 등록한다. 버전에 따라 메뉴 이름이 조금 다를 수 있다.

| 입력 항목 | 값 |
| --- | --- |
| Kind | Username with password |
| Scope | Global |
| Username | `lim-bada` |
| Password | GitHub PAT |
| ID | `github-read` |

공개 저장소는 인증 없이 clone할 수도 있다. 교안대로 자격 증명을 연습하려면 위 항목을 등록해서 선택한다.

**성공 기준:** Credentials 목록에 `github-read`가 보인다.

## 5. GitHub 파일을 읽는 Pipeline — PDF 84쪽, 100~103쪽

1. 새로운 **Pipeline** Item을 만들고 이름을 `devops-lab-ci`로 정한다.
2. 아래 표대로 설정하고 저장한다.
3. **Build Now**로 수동 실행한다.

| 설정 항목 | 값 |
| --- | --- |
| Definition | Pipeline script from SCM |
| SCM | Git |
| Repository URL | `https://github.com/lim-bada/devops-lab.git` |
| Credentials | `github-read` |
| Branch Specifier | `*/main` |
| Script Path | `ch03-jenkins/Jenkinsfile.hello` |

Script Path는 GitHub 저장소 최상위에서부터의 파일 경로다. 맥의 절대 경로를 입력하지 않는다.
교안 캡처의 `Jenkinsfile`은 파일이 저장소 최상위에 있을 때의 예다. 이 샘플은 폴더 안에 있으므로 위 경로를 입력한다.

**성공 기준:** GitHub에서 파일을 가져와 Build → Test → Deploy를 실행하고 `Finished: SUCCESS`로 끝난다. 자동 소스 가져오기 단계인 `Declarative: Checkout SCM`이 추가로 보일 수도 있다.

## 6. push할 때 자동 실행 — PDF 88쪽, 96~103쪽

Jenkins의 로컬 주소 `localhost:8080`은 GitHub에서 직접 접근할 수 없으므로 ngrok으로 공개 주소를 연결한다.

ngrok이 설치되어 있지 않다면 맥에서 설치한다.

```bash
brew install ngrok
```

ngrok 계정 대시보드의 본인 Authtoken을 등록한다. `YOUR_NGROK_AUTHTOKEN`을 바꿔 입력한다. 이 토큰은 GitHub PAT와도 다르다.

```bash
ngrok config add-authtoken "YOUR_NGROK_AUTHTOKEN"
ngrok http 8080
```

ngrok이 실행되는 터미널은 켜 둔다. 화면에 표시되는 HTTPS 공개 주소를 사용한다.

GitHub **실습 저장소** → Settings → Webhooks → Add webhook에서 설정한다.

| 항목 | 값 |
| --- | --- |
| Payload URL | `https://본인-ngrok-주소/github-webhook/` |
| Content type | `application/json` |
| 이벤트 | Just the push event |
| Active | 체크 |

Payload URL 마지막의 `/`까지 입력한다. 공개 주소가 바뀌면 이 URL도 바꾼다.
Jenkins의 `devops-lab-ci → 구성 / Configure → Build Triggers`에서 **GitHub hook trigger for GITScm polling**을 체크하고 저장한다.

Git 저장소 최상위에서 아래 명령으로 파일 변경 없이 테스트용 commit을 만들고 push한다.

```bash
git commit --allow-empty -m "test: trigger Jenkins webhook"
git push origin main
```

**성공 기준:** GitHub webhook의 Recent Deliveries에서 `push` 전달이 200으로 성공하고, Jenkins에 새 빌드가 자동 생성된다. webhook을 처음 추가할 때의 `ping` 성공과 실제 `push`에 따른 빌드 시작을 각각 확인한다.

## 7. 실제 앱 테스트·이미지 빌드·Harbor 업로드 — PDF 104~106쪽

GitHub에 연결한 로컬 저장소에서 [Jenkinsfile](Jenkinsfile)의 `IMAGE`를 확인한다. 본인 설정인 **3반 `g094`**의 주소가 이미 들어 있으므로 그대로 사용한다.

```groovy
environment {
    IMAGE = 'harbor.skala-gj.com/skala-gj3/g094-devops-lab-app'
}
```

이 주소에서 `skala-gj3`는 3반의 Harbor 프로젝트이고, `g094-devops-lab-app`은 본인 이미지 repository다.

Jenkins Credentials에 다음 항목을 추가한다.

| 입력 항목 | 값 |
| --- | --- |
| Kind | Username with password |
| Username | 3반 계정 `skala-gj3` |
| Password | 반별 안내문의 Harbor 비밀번호 |
| ID | **`harbor-login`** |

Jenkinsfile이 `harbor-login`이라는 ID로 이 항목을 참조한다. GitHub용 `github-read`와 따로 등록한다.

3단계에서 현재 파일을 그대로 올렸다면 추가 commit은 필요 없다. 이후 Jenkinsfile을 수정한 경우에는 아래처럼 commit하고 push한다.

```bash
git add ch03-jenkins/Jenkinsfile
git commit -m "ci: configure my Harbor image"
git push origin main
```

Jenkins의 `devops-lab-ci → 구성 / Configure`에서 Script Path를 **`ch03-jenkins/Jenkinsfile`**로 바꾸고 저장한다. **Build Now**로 실행한다.

| stage | 실제 작업 | 성공 확인 |
| --- | --- | --- |
| Version | commit SHA 앞 7자리를 태그로 정함 | 로그의 `image tag = ...` |
| Test | `app/`에서 Gradle 테스트 | 테스트 통과, JUnit 결과 |
| Image | `linux/amd64` Docker 이미지 빌드 | 이미지 빌드 완료 |
| Push | Harbor 로그인 후 이미지 업로드 | 업로드 성공, 전체 SUCCESS |

`Push`는 main 브랜치에서만 실행된다. Apple Silicon에서도 이 코드는 광주 클러스터에 맞춰 amd64 이미지를 만든다.
자동 Checkout SCM까지 포함하면 다섯 단계로 보일 수 있다.

브라우저에서 `https://harbor.skala-gj.com` 접속 → 3반 프로젝트 `skala-gj3` → `g094-devops-lab-app` → commit SHA 7자리 태그를 확인한다.

**3장 완료 기준:** 실제 앱 Pipeline이 SUCCESS이고, 본인 Harbor repository에 해당 태그가 있다.
현재 Jenkinsfile은 이미지 태그에 SHA를 쓰지만 `APP_VERSION` build-arg는 전달하지 않는다. 이 이미지로 앱을 실행했을 때 응답의 `version`이 `local`이어도 위 업로드 확인과는 별개다.

## 8. 막혔을 때 확인 순서

| 증상 | 확인·대응 |
| --- | --- |
| `no configuration file provided` | `ch03-jenkins`에서 Compose 명령을 실행했는지 확인 |
| `8080 ... address already in use` | `docker ps`로 Jenkins 실행 여부 확인. `bootRun` 등 별도 앱이면 해당 터미널에서 종료 |
| Jenkins가 아직 열리지 않음 | `docker compose logs --tail=100 jenkins`로 시작 로그 확인 |
| 플러그인 다운로드 실패 | 교안 안내대로 핫스팟 전환 후 실패 항목 Retry |
| `checkout scm` 관련 오류 | 화면 입력 연습은 `Jenkinsfile.hello`, SCM 연습은 `Pipeline script from SCM` 설정 확인 |
| GitHub 파일을 찾지 못함 | 저장소 URL, main에 push했는지, Script Path 확인 |
| Git 인증 실패 | `github-read` 토큰의 대상 저장소·만료·읽기 권한 확인 |
| webhook 연결 실패 | ngrok 실행 여부, 현재 공개 URL, `/github-webhook/` 경로 확인 |
| webhook 200인데 자동 실행 안 됨 | trigger 체크, SCM URL, 새 push 여부 확인. 먼저 Build Now 성공 확인 |
| `docker: not found` | `ch03-jenkins`에서 `docker compose up -d --build` 후 `docker exec jenkins docker version` 확인 |
| `./gradlew: Permission denied` | 저장소 최상위에서 `git update-index --chmod=+x app/gradlew` 후 commit·push |
| `harbor-login`을 찾지 못함 | Credentials ID와 Global 등록 위치 확인 |
| Harbor 로그인 `unauthorized` | Harbor 계정·비밀번호 확인 |
| Harbor push `401 Unauthorized` | IMAGE의 프로젝트가 본인 반 `skala-gj<N>`인지 확인 |

빌드 실패 시 **Console Output에서 처음 실패한 stage의 오류**를 확인한다. 마지막 정리 작업의 로그보다 앞쪽에 원인이 있을 수 있다.

## 9. 실습을 잠시 멈출 때

ngrok 터미널에서 Ctrl+C로 터널을 종료한다. Jenkins도 멈추려면 다음 명령을 사용한다.

```bash
cd /Users/limhaean/workspace/16-devops-lab/ch03-jenkins
docker compose stop
```

다시 시작할 때는 `docker compose start`를 사용한다.
`docker compose down -v`는 Jenkins 설정·계정·job을 저장한 볼륨까지 지우므로, 설정을 유지하며 중단할 때는 사용하지 않는다.

## 10. 3장 다음 진행 경로

| 장 | PDF 뷰어 쪽수 | 다음 실습 | 완료 기준 |
| --- | --- | --- | --- |
| 4장 Actions | 109~135 | [../ch04-actions/README.md](../ch04-actions/README.md)의 hello → 앱 CI → Harbor push | Actions 성공, Harbor 태그 생성 |
| 5장 Tekton | 136~161 | [../ch05-tekton/README.md](../ch05-tekton/README.md)의 Task → Pipeline → CI. 로컬 Kubernetes에서 실행 | PipelineRun Succeeded |
| 6장 Argo CD | 162~191 | 광주 교안 경로인 [../gwangju/README.md](../gwangju/README.md)의 manifest 저장소 준비 → nginx 등록 → 태그 변경 | Synced·Healthy, nginx 응답 |
| 7장 연결 | 192~215 | [../ch07-capstone/README.md](../ch07-capstone/README.md)의 Actions → Harbor → manifest 수정 → Argo CD | push 후 공개 앱 응답의 version 변경 |

5장의 Tekton은 로컬 kind/docker-desktop 클러스터를 사용한다. 광주판 6~7장은 제공된 광주 Argo CD와 클러스터를 사용한다. `ch06-argocd/README.md`의 로컬 Argo CD 설치 명령과 광주판의 접속 경로를 구분해서 진행한다.

## 확인한 자료

- 로컬 PDF 교안의 3장 전체와 1·2일차 실습 순서.
- [3장 README](README.md), Compose 설정, Dockerfile, 두 Jenkinsfile, 앱 빌드 설정.
- [Jenkins Pipeline 시작 문서](https://www.jenkins.io/doc/book/pipeline/getting-started/): UI 입력과 SCM 연결.
- [Jenkins GitHub plugin 문서](https://plugins.jenkins.io/github/): 수동 webhook URL과 GitHub hook trigger.
- [GitHub webhook 생성 문서](https://docs.github.com/en/webhooks/using-webhooks/creating-webhooks): 웹 화면에서 webhook 등록과 이벤트 선택.
- [GitHub PAT 문서](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens): 저장소 선택과 토큰 권한.
- [ngrok 공식 설치·실행 안내](https://ngrok.com/use-cases/share-localhost): Homebrew 설치, authtoken 등록, 로컬 포트 공개.
