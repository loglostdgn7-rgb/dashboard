# GitHub 실시간 활동 대시보드

GitHub 공개 이벤트 API 데이터를 받아서 실시간으로 보여주는 대시보드를 만들어 봤습니다
Spring Boot의 SseEmitter로 실시간 푸시를 구현하고, 비동기 UI를 만들면서 생긴 문제들을 해결하는 게 목표였습니다

<br>

**배포 사이트**
> http://[여기에-배포한-사이트-주소-입력]
 
**최종 배포 사이트 (랜딩 페이지)**
> https://loglostdgn7-rgb.github.io/#portfolio

---

**주요 기능**

* 실시간 데이터 갱신: SSE (Server-Sent Events)를 써서, 서버에서 데이터가 오면 새로고침 없이 바로 업데이트되게 했습니다
* 이벤트 통계 시각화: Chart.js로 이벤트 통계를 막대 차트로 그렸습니다
* 비동기 페이지네이션: 자바스크립트로 "이벤트 리스트" 부분만 새로고침 안 하고 페이징되게 만들었습니다
* Top 랭킹: 최근 7일간 활동이 많은 리포지토리랑 사용자를 뽑아봤습니다

---

**사용 기술**

| 구분 | 기술 스택 |
| --- | --- |
| Backend | Spring Boot, Spring Web, Spring Caching, MyBatis, SseEmitter |
| Frontend | Thymeleaf, JavaScript(ES6+), Chart.js, htmx (도입 시도) |
| Database | MySQL, H2 (테스트용) |
| Test | JUnit 5, Mockito, Spring Boot Test |
| ETC | Git, GitHub, IntelliJ IDEA |

---

**아키텍처 (데이터 흐름)**

1. 데이터 수집 (Scheduler): Spring 스케줄러가 주기적으로 GitHub API를 불러서 데이터를 모읍니다
2. DB 저장 (MyBatis): 모은 데이터를 DB에 넣습니다
3. 데이터 처리 (Service): 통계나 랭킹 데이터를 만듭니다
4. 실시간 푸시 (SSE): 새 데이터가 생기면 SseService가 "update" 이벤트를 클라이언트로 쏴줍니다
5. 클라이언트 갱신 (JS): main.js의 EventSource가 "update" 이벤트를 받으면, 화면을 고칩니다

---

**고생했던 점 (트러블슈팅)**

이 프로젝트는 기능을 그냥 "완성"했다기보다, 만들면서 만난 문제들을 해결하려고 애쓴 기록에 가깝습니다

**1. 비동기 페이지네이션: 복잡성과의 싸움**

SSE로 실시간 갱신되는 totalCount (총 아이템 수)를 가지고 "마지막 페이지 가기" 버튼을 만드는데, 이상하게 자꾸 버그가 났습니다

* **잘못된 추측: "캐시(Cache) 문제일 것이다"**
    * 문제가 뭐였냐면: "마지막 페이지" 계산이 실시간으로 바뀐 totalCount가 아니라, 예전 totalCount 값으로 계산됐습니다 사용자 경험을 조금이라도 올려보려고 캐시를 썼던 게 문제인 줄 알았습니다
    * 처음엔 이랬습니다: "이거 캐시 문제다" DB에서 최신 값을 못 가져오고, Spring Caching에 남은 예전 데이터를 쓰는 줄 알았습니다
    * 그래서 해본 것들: @Cacheable 어노테이션도 지워보고, @CachePut도 써보고... CacheManager로 수동으로 캐시를 지우는 짓도 해봤습니다 근데 알고 보니 이 고생은 문제랑 "전혀" 관련이 없었습니다

* **두 번째 시도: "htmx 도입의 실패"**
    * 깨달음: 한참 뒤에야 이게 캐시 문제가 아니라, 클라이언트(JS)랑 서버(SSE)가 "상태"를 똑같이 못 맞추는 문제인 걸 알았습니다
    * 새로운 시도: "자바스크립트가 복잡하니까 htmx를 써보자!" hx-get으로 부분 렌더링은 됐는데, EventSource로 들어오는 totalCount 값을 htmx가 알아채질 못했습니다 결국 페이지 블록이 깨지고 코드가 엉망이 됐습니다

* **결국 해결: 바닐라 JS로 다시 만들기**
    * 결론: htmx가 만능이 아니란 걸 깨닫고... 그냥 처음부터 다시 하기로 했습니다
    * 해결: htmx 코드를 다 지우고, "다시 바닐라 JavaScript로" 페이지네이션 로직을 싹 다 다시 짰습니다
    * 결과: 고생은 했지만... 덕분에 SSE 갱신 타이밍이랑 페이지네이션 UI가 언제 그려지는지 확실하게 제어할 수 있게 됐습니다

**2. 페이지네이션: JS로 만든 버튼이 클릭 안 되는 문제**

바닐라 JS로 다 만들고 나서, "또" 문제가 생겼습니다 페이지를 누르면 자꾸 상단으로 갔습니다

* 현상: SSE 이벤트로 페이지네이션 버튼(a 태그)들을 innerHTML로 새로 그렸더니, 이 버튼들에 붙어있던 htmx의 "hx-get" 이벤트가 "작동을 안 했습니다"
* 문제: HTMX는 페이지가 맨 처음 열릴 때만 hx- 속성을 스캔하는데, 자바스크립트로 나중에 만든 HTML은 htmx가 모르는 "죽은" 상태였던 겁니다
* 해결: updatePagination 함수 (innerHTML로 화면 바꾸는 함수) 맨 마지막에 "htmx.process(paginationContainer);" 이 코드를 한 줄 넣었습니다 "이 컨테이너 안에 새 HTML 생겼으니까, 다시 스캔해서 활성화시켜!"라고 htmx에게 알려주는 겁니다
* 결과: 이 한 줄로 동적으로 만든 버튼들이 htmx랑 다시 연결됐고, 페이지네이션이 드디어 제대로 작동했습니다 만세!

**3. 테스트 환경: 자꾸 꼬이는 테스트 DB 문제**

서비스 로직을 테스트(@SpringBootTest)하는데, 자꾸 "테스트 DB 테이블 구조가 꼬이거나" 데이터가 중복돼서 테스트가 실패했습니다

* 문제: 테스트는 몇 번을 돌려도 똑같아야 하는데, 그렇질 못했습니다
* 해결: application-test.properties를 만들어서 H2 메모리 DB를 쓰게 바꿨습니다 그리고 "schema.sql" 파일을 만들어서, 테스트 시작할 때마다 항상 똑같은 테이블을 새로 만들도록 했습니다 덕분에 깨끗한 환경에서 테스트할 수 있게 됐습니다

---

**테스트 코드**

위에서 고생한 것들을 바탕으로, 주요 로직에 테스트 코드를 좀 넣어봤습니다 (다는 못했지만...)

* **DataReprocessServiceTest (서비스 통합 테스트)**
    * schema.sql로 만든 H2 DB 환경에서 서비스 로직이 잘 도는지 테스트했습니다 (페이지네이션 offset 계산, 통계 GROUP BY 같은 거)
* **MainControllerTest (컨트롤러 단위 테스트)**
    * 컨트롤러만 따로 떼서(@WebMvcTest), 서비스는 가짜(@MockBean)로 만들고 테스트했습니다 (메인 페이지("/") 요청 시 뷰 이름이나 모델이 잘 넘어가는지)

---

**로컬에서 실행하기**

1. 이 리포지토리를 클론합니다
2. src/main/resources 경로에 application-secret.properties 파일을 만들고, DB(MySQL) 연결 정보를 넣습니다 (H2 쓸 거면 생략 가능)
3. DashboardApplication을 실행합니다
4. http://localhost:8080에 접속합니다
