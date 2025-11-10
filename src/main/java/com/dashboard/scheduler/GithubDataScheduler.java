package com.dashboard.scheduler;

import com.dashboard.dto.*;
import com.dashboard.mapper.EventMapper;
import com.dashboard.service.DataReprocessService;
import com.dashboard.service.GithubApiService;
import com.dashboard.service.SseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class GithubDataScheduler {
    @Autowired
    private GithubApiService githubApiService;
    @Autowired
    private EventMapper eventMapper;
    @Autowired
    private SseService sseService;
    @Autowired
    private DataReprocessService dataReprocessService;

    private final int PAGE_SIZE = 10;
    @Autowired
    private CaffeineCacheManager cacheManager;


    //1분마다 받아오기
    @Scheduled(fixedRate = 20000)
    public void updateGithubData() {
        System.out.println("데이터 수집 실행중...");

        List<EventDTO> eventList = githubApiService.getGithubActivity();

        if (eventList != null && !eventList.isEmpty()) {
            //집어넣기
            eventMapper.insertEventList(eventList);
            System.out.println(eventList.size() + "개의 새 이벤트 저장: " + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));


            //캐시 수동업데이트
            int totalCount = dataReprocessService.getTotalEventCount_Fresh();


            List<EventStatsDTO> eventStats = dataReprocessService.getEventStats_Fresh();
            List<EventDTO> newEventList = dataReprocessService.findEventsForPage(1, PAGE_SIZE);
            List<RepoDTO> topRepos = dataReprocessService.getTopRepos_Fresh();
            List<ActorDTO> topUsers = dataReprocessService.getTopUsers_Fresh();


            // 차트 데이터 생성
            List<String> chartLabels = eventStats.stream().map(EventStatsDTO::getType).toList();
            List<Integer> chartData = eventStats.stream().map(EventStatsDTO::getCount).toList();

            //SSE 전송용 DTO 생성
            UpdateResponseDTO updateData = new UpdateResponseDTO(
                    topRepos, topUsers, eventStats, newEventList, chartLabels, chartData, totalCount
            );

            // SSE 전송 알림
            sseService.sendUpdateNotification(updateData);
            System.out.println("SSE 갱신 전송 완료" + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));


            try {
                Cache statsCache = cacheManager.getCache("eventStats");
                Cache countCache = cacheManager.getCache("totalEventCount");
                Cache topReposCache = cacheManager.getCache("topRepos");
                Cache topUsersCache = cacheManager.getCache("topUsers");

                //캐시 수동 지우기
                if (statsCache != null) statsCache.clear();
                if (countCache != null) countCache.clear();
                if (topReposCache != null) topReposCache.clear();
                if (topUsersCache != null) topUsersCache.clear();

                System.out.println("캐시 비우기 완료");
            } catch (Exception e) {
                System.out.println("예외 발생: " + e.getMessage());
            }

        } else {
            System.out.println("새 이벤트 없음");
        }
    }


    //7일 넘은거 지우기. 매일.
    //초 분 시 일 월 요일
    @CacheEvict(value = {"totalEventCount", "eventStats", "topRepos", "topUsers"}, allEntries = true)
    @Scheduled(cron = "0 0 4 * * *")
    public void deleteOldEvents() {
        System.out.println("오래된 데이터 삭제 실행");

        //삭제
        eventMapper.deleteOldEvents();

        System.out.println("7일 이상된 데이터 삭제 완료");
    }
}
