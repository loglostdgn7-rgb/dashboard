package com.dashboard.service;

import com.dashboard.dto.EventDTO;
import com.dashboard.dto.EventStatsDTO;
import com.dashboard.dto.RepoDTO;
import com.dashboard.mapper.EventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
public class DataReprocessServiceTest {

    @MockBean
    private GithubApiService githubApiService;

    @MockBean
    private SseService sseService;

    @Autowired
    private DataReprocessService dataReprocessService;

    @Autowired
    private EventMapper eventMapper;

    private EventDTO testEvent1;
    private EventDTO testEvent2;
    private EventDTO testEvent3;

    @BeforeEach
    void setUp() {
        RepoDTO repo1 = new RepoDTO();
        repo1.setName("userA/repo1");

        RepoDTO repo2 = new RepoDTO();
        repo2.setName("userB/repo2");

        testEvent1 = new EventDTO();
        testEvent1.setId("event1_from_api");
        testEvent1.setType("PushEvent");
        testEvent1.setRepo(repo1);
        testEvent1.setCreatedAt("2025-11-10T10:00:00Z"); // 최신

        testEvent2 = new EventDTO();
        testEvent2.setId("event2_from_api");
        testEvent2.setType("WatchEvent");
        testEvent2.setRepo(repo2);
        testEvent2.setCreatedAt("2025-11-09T10:00:00Z"); // 중간

        testEvent3 = new EventDTO();
        testEvent3.setId("event3_from_api");
        testEvent3.setType("PushEvent");
        testEvent3.setRepo(repo1);
        testEvent3.setCreatedAt("2025-11-08T10:00:00Z"); // 가장 과거
    }

    @Test
    @DisplayName("DB에 데이터를 3개 넣고, getTotalEventCount가 3을 반환하는지 확인")
    void testGetTotalEventCount() {
        // given
        eventMapper.insertEventList(List.of(testEvent1, testEvent2, testEvent3));

        // when
        int totalCount = dataReprocessService.getTotalEventCount();

        // then
        assertThat(totalCount).isEqualTo(3);
    }

    @Test
    @DisplayName("DB에 데이터를 3개 넣고, 1페이지(5개) 조회 시 3개가 올바르게 조회되는지 확인 (정렬 검증)")
    void testFindEventsForPage_FirstPage() {

        eventMapper.insertEventList(List.of(testEvent1, testEvent2, testEvent3));


        List<EventDTO> result = dataReprocessService.findEventsForPage(1, 5);


        assertThat(result).hasSize(3);


        assertThat(result.get(0).getCreatedAt()).isEqualTo("2025-11-10T10:00:00Z"); // 최신
        assertThat(result.get(0).getType()).isEqualTo("PushEvent");

        assertThat(result.get(1).getCreatedAt()).isEqualTo("2025-11-09T10:00:00Z");

        assertThat(result.get(2).getCreatedAt()).isEqualTo("2025-11-08T10:00:00Z"); // 가장 과거
    }

    @Test
    @DisplayName("DB에 데이터를 3개 넣고, 2페이지(2개씩) 조회 시 마지막 1개가 조회되는지 확인 (OFFSET 계산 검증)")
    void testFindEventsForPage_Pagination() {

        eventMapper.insertEventList(List.of(testEvent1, testEvent2, testEvent3));


        List<EventDTO> result = dataReprocessService.findEventsForPage(2, 2);


        assertThat(result).hasSize(1);


        assertThat(result.get(0).getCreatedAt()).isEqualTo(testEvent3.getCreatedAt()); // 3번째 데이터
    }

    @Test
    @DisplayName("DB에 PushEvent 2개, WatchEvent 1개를 넣고, getEventStats가 통계를 올바르게 반환하는지 확인")
    void testGetEventStats() {

        eventMapper.insertEventList(List.of(testEvent1, testEvent2, testEvent3));


        List<EventStatsDTO> stats = dataReprocessService.getEventStats();


        assertThat(stats).hasSize(2);


        assertThat(stats)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder( // 순서 상관없이
                        new EventStatsDTO("PushEvent", 2),
                        new EventStatsDTO("WatchEvent", 1)
                );
    }
}