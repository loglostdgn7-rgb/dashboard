// 차트
const PAGE_SIZE = 10;
const PAGE_BLOCK_SIZE = 5
let myBarChart;

document.addEventListener('DOMContentLoaded', () => {
    const canvas = document.querySelector('.chart-bar');


    if (!canvas) {
        console.log("차트캔버스를 찾을 수 없음. 차트 초기화 스킵");
        return;
    }

    // html data- 에서 데이터 가져오기
    const labelsString = canvas.dataset.labels;
    const dataString = canvas.dataset.data;

    if (!labelsString || !dataString) {
        console.error("차트 데이터 찾을 수 없음");
        return;
    }


    const labelsContent = labelsString.substring(1, labelsString.length - 1);
    const dataContent = dataString.substring(1, dataString.length - 1);


    const labels = labelsContent.length > 0 ? labelsContent.split(',').map(content => content.trim()) : [];
    const data = dataContent.length > 0 ? dataContent.split(',').map(content => content.trim()).map(Number) : [];

    const dataCount = labels.length;
    const visibleBarCount = 3; // "먼저 보여줄" 막대 개수

    // 차트 컨테이너 너비 동적 계산
    const innerContainer = document.querySelector('.chart-canvas-container');
    const outerContainer = document.querySelector('.stats-chart');

    if (!innerContainer || !outerContainer) {
        console.error("차트 엘레먼트 찾을 수 없음");
        return;
    }

    // 데이터가 3개보다 많을 때만 스크롤 적용
    if (dataCount > visibleBarCount) {
        const containerWidth = outerContainer.clientWidth;
        const newWidth = (dataCount / visibleBarCount) * containerWidth;
        innerContainer.style.width = newWidth + 'px';
    }

    //Throttle (1px 이동 마다 무한히 이벤트 발생 방지)
    function throttle(someFunction, limit) {
        let isTheGateClose;
        return function () {
            const args = arguments;
            const context = this;
            if (!isTheGateClose) {
                someFunction.apply(context, args);
                isTheGateClose = true;
                setTimeout(() => isTheGateClose = false, limit);
            }
        };
    }

    function updateYAxis() {
        if (!myBarChart || !outerContainer) {
            return;
        }
        const scrollLeft = outerContainer.scrollLeft;
        const scrollWidth = outerContainer.scrollWidth;
        const clientWidth = outerContainer.clientWidth;
        const totalBars = data.length;
        const startRatio = scrollLeft / scrollWidth;
        const endRatio = (scrollLeft + clientWidth) / scrollWidth;
        const startIndex = Math.max(0, Math.floor(totalBars * startRatio));
        const endIndex = Math.min(totalBars - 1, Math.ceil(totalBars * endRatio));
        const visibleData = data.slice(startIndex, endIndex + 1);

        if (visibleData.length === 0) {
            return;
        }
        const maxVisibleValue = Math.max(...visibleData);
        const newYMax = maxVisibleValue * 1.2;
        myBarChart.options.scales.y.max = newYMax;
        myBarChart.update('none');
    }

    // 차트
    // JSON.parse() 대신 위의 labels, data 변수들 사용
    const ctx = canvas.getContext("2d");
    myBarChart = new Chart(ctx, { // 전역 변수 myBarChart에 할당
        type: "bar",
        data: {
            labels: labels,
            datasets: [{
                label: "이벤트 횟수",
                data: data,
                backgroundColor: "#ff9e49",
                borderColor: "#d9802e",
                borderWidth: {
                    top: 0,
                    right: 40,
                    bottom: 0,
                    left: 0
                },
                borderRadius: 3,
                borderSkipped: false
            }]
        },
        options: {
            animation: {
                y: {
                    duration: 1200,
                    easing: "easeOutCubic"
                }
            },
            responsive: true,
            maintainAspectRatio: false, //캔버스 고유 사이즈 유지 무시
            scales: {
                y: {
                    beginAtZero: true
                }
            },
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    interaction: {
                        mode: "index",
                        intersect: false
                    }
                }
            }
        }
    });

    // 스크롤 리스너 연결
    if (dataCount > visibleBarCount) {
        outerContainer.addEventListener('scroll', throttle(updateYAxis, 250));
    }


    //연결시도
    const eventSource = new EventSource("/subscribe");
    console.log("표준 EventSource 연결 시도...");


    eventSource.addEventListener("connect", (event) => {
        console.log("SSE 연결 성공:", event.data);
    });


    //업데이트(갱신)
    eventSource.addEventListener("update", (event) => {
        console.log("SSE 갱신 데이터 받음");
        // console.log("수신된 원본 데이터:", event.data);

        let data;
        try {
            data = JSON.parse(event.data);
        } catch (e) {
            console.error("SSE 데이터 JSON 파싱 실패:", e);
            // console.error("파싱 실패한 데이터: ", event.data);
            showUpdateModal("데이터 수신 오류 (콘솔 확인)");
            return;
        }

        showUpdateModal("갱신되었습니다");

        if (myBarChart) {
            myBarChart.data.labels = data.chartLabels;
            myBarChart.data.datasets[0].data = data.chartData;
            myBarChart.update('none');
        }

        updateStatsList(data.eventStats);
        updateEventList(data.eventList);
        updateTopRepos(data.topRepos);
        updateTopUsers(data.topUsers);

        if (data.totalCount !== undefined) {
            updatePagination(data.totalCount);
        }
    });

    // 3. 에러 처리
    eventSource.onerror = (error) => {
        console.error("EventSource 오류 발생:", error);

        // 오류 발생 시 자동으로 재연결을 시도함.
        // 서버가 완전히 닫혔으면 여기서 연결을 명시적으로 닫을 수 있다고함;

        // console.log("SSE 연결 실패. 재연결 시도를 중지합니다.서버가 종료된 것 같습니다");
        // eventSource.close(); //엔진엑스가 1일동안 연결을 끊지 않으니 다지 재연결 할거 같음
    };

}); // DOMContentLoaded 끝


// SSE 헬퍼 함수
function updateStatsList(stats) {
    const container = document.querySelector(".stats-list-content");

    if (!container) return;

    // 기존 아이템 삭제
    container.querySelectorAll(".grid-item, .grid-none-item").forEach(item => item.remove());

    // grid-head는 .stats-list-content 안에 있으므로, 그 뒤에 아이템 추가
    if (stats && stats.length > 0) {
        stats.forEach(stat => {
            const item = document.createElement("div");

            item.className = "grid-item";
            item.innerHTML = `
                <span>${stat.type}</span>
                <span>${stat.count}건</span>
            `;
            container.appendChild(item);
        });
    } else {
        const noneItem = document.createElement("div");

        noneItem.className = "grid-none-item";
        noneItem.textContent = "통계 데이터가 없습니다";
        container.appendChild(noneItem);
    }
}

function updateEventList(eventList) {
    const container = document.querySelector(".event-list-section .grid-container");
    if (!container) return;

    container.querySelectorAll(".grid-item, .grid-none-item").forEach(item => item.remove());

    if (eventList && eventList.length > 0) {
        eventList.forEach(event => {
            const item = document.createElement("div");

            item.className = "grid-item";

            const repoName = event.repo ? event.repo.name : 'N/A';


            const koreaTime = kstTime(event.created_at);

            item.innerHTML = `
                <span>${event.type}</span>
                <span>${repoName}</span>
                <span>${koreaTime}</span> 
            `;
            container.appendChild(item);
        });
    } else {
        const noneItem = document.createElement("div");
        noneItem.className = "grid-none-item";
        noneItem.textContent = "데이터가 없습니다. 수집중...";
        container.appendChild(noneItem);
    }
}

//갱신 알림 모달
function showUpdateModal(message) {
    const modal = document.createElement("div");

    modal.textContent = message;
    modal.style.position = "fixed";
    modal.style.top = "50%";
    modal.style.left = "50%";
    modal.style.transform = "translate(-50%, -50%)";
    modal.style.backgroundColor = "rgba(0, 0, 0, 0.75)";
    modal.style.color = "white";
    modal.style.padding = "15px 30px";
    modal.style.borderRadius = "8px";
    modal.style.zIndex = "1001";
    modal.style.opacity = "1";
    modal.style.transition = "none";

    document.body.appendChild(modal);

    // 천천히 5초동안 사라지기
    setTimeout(() => {
        modal.style.transition = "opacity 5s ease-in-out";
    }, 10);

    // 3초 보여주고 opacity 0 만든다음, 5초뒤에 removeChild
    setTimeout(() => {

        modal.style.opacity = "0";

        setTimeout(() => {

            if (modal.parentNode) {
                modal.parentNode.removeChild(modal);
            }
        }, 5000);
    }, 3000);
}


function kstTime(dateString) {
    if (!dateString) return "N/A";

    try {
        const date = new Date(dateString);

        return date.toLocaleString('sv-SE', {
            timeZone: "Asia/Seoul",
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit",
            hourCycle: "h23"
        });
    } catch (error) {
        console.error("날짜 변환 오류:", dateString, error);
        return dateString;
    }
}


function updateTopRepos(topRepos) {
    const container = document.querySelector(".popular-repos");
    if (!container) return;

    // 기존 아이템 삭제
    container.querySelectorAll("div").forEach(item => item.remove());

    if (topRepos && topRepos.length > 0) {

        topRepos.forEach(repo => {
            const item = document.createElement("div");
            item.className = "grid-item";

            item.innerHTML = `
                <span>${repo.name}</span>
                <span>${repo.count}건</span>
            `;
            container.appendChild(item);
        });
    } else {
        console.log("데이터 없음");
    }
}


function updateTopUsers(topUsers) {
    const container = document.querySelector(".popular-users");
    if (!container) return;

    container.querySelectorAll("div").forEach(item => item.remove());

    if (topUsers && topUsers.length > 0) {
        topUsers.forEach(user => {

            const item = document.createElement("div");
            item.className = "grid-item";

            const imageUrl = user.avatarUrl || user.avatar_url || "";

            item.innerHTML = `
                <img src="${imageUrl}" alt="${user.login}"/>
                <span>${user.login}</span>
                <span>${user.count}건</span>
            `;
            container.appendChild(item);
        });
    } else {
        console.log("데이터 없음");
    }
}


//페이지네이션을 다시 계산, 재조립(?) 해줘야 한다
//그렇지 않으면 마지막 페이지를 눌렀을때 갱신 데이터를 인식하지 못하고,
//갱신되기 이전의 마지막 페이지로 이동하고, 뒤에 페이지 여분이 남게 된다...
function updatePagination(totalCount) {

    const newTotalPages = Math.ceil(totalCount / PAGE_SIZE);
    const paginationContainer = document.querySelector(".pagination");


    if (!paginationContainer) {

        if (paginationContainer && newTotalPages <= 1) {
            paginationContainer.innerHTML = "";

            return;
        }

        if (!paginationContainer) {
            console.log("페이지네이션 컨테이너를 찾을 수 없음");

            return;
        }
    }


    const currentPageEl = paginationContainer.querySelector("a.active");
    const currentPage = currentPageEl ? parseInt(currentPageEl.textContent, 10) : 1;
    const startPage = Math.floor((currentPage - 1) / PAGE_BLOCK_SIZE) * PAGE_BLOCK_SIZE + 1;

    let endPage = startPage + PAGE_BLOCK_SIZE - 1;

    if (endPage > newTotalPages) {
        endPage = newTotalPages;
    }

    const hasPrev = startPage > 1;
    const hasNext = endPage < newTotalPages;
    const prevPage = startPage - 1; // ◀ 버튼용
    const nextPage = endPage + 1;   // ▶ 버튼용
    const hxTarget = ".event-list-section";
    const hxSwap = "innerHTML";
    const hxPush = "false";

    // 링크 생성기
    const createLink = (page, text, cssClass = "") => {
        const href = `/?page=${page}`;
        const hxGet = `/?page=${page}`;


        let activeClass = (page === currentPage && !cssClass) ? 'active' : cssClass;

        return `<a href="${href}" 
                   class="${activeClass}"
                   hx-get="${hxGet}"
                   hx-target="${hxTarget}" 
                   hx-swap="${hxSwap}" 
                   hx-push-url="${hxPush}">${text}</a>`;
    };

    let paginationHTML = '';


    if (hasPrev) {
        paginationHTML += createLink(1, "시작으로");
        paginationHTML += createLink(prevPage, "◀");
    }

    // 페이지 숫자 버튼
    for (let i = startPage; i <= endPage; i++) {
        paginationHTML += createLink(i, i);
    }


    if (hasNext) {
        paginationHTML += createLink(nextPage, "▶");
        paginationHTML += createLink(newTotalPages, "끝으로");
    }

    // 적용
    paginationContainer.innerHTML = paginationHTML;

    // htmx 라이브러리가 로드되었는지 확인
    // htmx 는 자동 갱신된 혹은 새로 생신된 것을 인식하지 못한다. 그래서 process 라는걸 써줘서 새로 인식시켜줘야한다
    // 그렇지 않으면 데이터가 갱신 된 후 페이지네이션을 클릭하면 주소가 ?page=1233처럼 바뀌면서 페이지가 상단으로 이동되어버린다
    if (htmx) {
        htmx.process(paginationContainer);
    }


}