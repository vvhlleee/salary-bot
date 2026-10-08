💰 Salary Manager Telegram Bot
텔레그램을 통해 개인 자산, 급여, 그리고 고정 지출을 스마트하게 관리할 수 있는 Spring Boot 기반의 텔레그램 봇 서비스입니다.
GitHub Actions를 활용한 CI/CD 자동 배포 파이프라인이 구축되어 있어, 코드 수정 후 푸시만 하면 오라클 클라우드 서버에 실시간으로 자동 반영됩니다.

🛠 Tech Stack
Language: Java 17

Framework: Spring Boot (Gradle)

Database: MySQL, Spring Data JPA

Bot API: Telegram Bots API

Infrastructure: Oracle Cloud Infrastructure (OCI, Ubuntu)

CI/CD: GitHub Actions, SCP, SSH

🚀 Key Features
지출 및 자산 관리: 텔레그램 채팅을 통해 간편하게 지출 내역을 기록하고 잔액을 조회

고정 지출 관리: 매월 나가는 고정 비용 항목 등록 및 관리

맞춤형 설정: 급여일, 알림 시간 등 사용자별 맞춤 설정 기능 제공

자동 배포 (CI/CD): main 브랜치 머지/푸시 시 오라클 클라우드 서버로 자동 빌드 및 배포

⚙️ CI/CD Architecture (GitHub Actions)
본 프로젝트는 GitHub Actions를 통해 다음과 같은 흐름으로 자동 배포됩니다.

Push: 개발자가 main 브랜치에 코드를 git push

Build: GitHub Actions 가상 환경에서 JDK 17과 Gradle을 이용해 .jar 파일 빌드

Transfer (SCP): 생성된 .jar 파일을 오라클 클라우드 서버로 안전하게 전송

Deploy (SSH): 오라클 서버에 원격 접속하여 systemd 서비스를 재시작(sudo systemctl restart salary-bot)하여 최신 코드 반영

📁 Project Structure
salary-manager/
├── .github/
│   └── workflows/
│       └── deploy.yml      # GitHub Actions 자동 배포 설정 파일
├── src/
│   ├── main/
│   │   ├── java/com/example/salarymanager/
│   │   │   ├── controller/ # 텔레그램 봇 핸들러 및 컨트롤러
│   │   │   ├── service/    # 비즈니스 로직 (TelegramBotService 등)
│   │   │   ├── repository/ # Spring Data JPA 인터페이스
│   │   │   └── entity/     # DB 엔티티 (User, Expense, Account 등)
│   │   └── resources/
│   │       └── application.yml
└── build.gradle

🔑 Environment Variables (GitHub Secrets)
GitHub Actions 자동 배포를 정상적으로 작동시키려면 레포지토리의 Settings ➡️ Secrets and variables ➡️ Actions에 다음 시크릿들이 등록되어 있어야 합니다.
Secret Name        Description
ORACLE_HOST        오라클 클라우드 인스턴스 퍼블릭 IP 주소
ORACLE_USER        서버 접속 사용자명 (예: ubuntu)
ORACLE_SSH_KEY     서버 접속용 SSH Private Key (.pem 내용 전체)
