package com.dashboard.controller;

import com.dashboard.service.DataReprocessService;
import com.dashboard.service.GithubApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;


import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(MainController.class)
public class MainControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private DataReprocessService dataReprocessService;
    @MockBean
    private GithubApiService githubApiService;
    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @DisplayName("메인 대시보드 페이지(/)를 요청하면, main 뷰를 반환하고 200 OK를 응답한다")
    public void testDashboard() throws Exception {

        //예상 결과 값
        given(dataReprocessService.getTotalEventCount()).willReturn(0);

        given(dataReprocessService.findEventsForPage(1, 5))
                .willReturn(Collections.emptyList());

        given(dataReprocessService.getEventStats())
                .willReturn(Collections.emptyList());

        String expectedJson = objectMapper.writeValueAsString(Collections.emptyList());


        // 실제검증
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("page/main"))
                .andExpect(model().attributeExists("pageData"))
                .andExpect(model().attribute("pageData", hasProperty("eventList", hasSize(0))))
                .andExpect(model().attributeExists("eventStats"))
                .andExpect(model().attribute("chartLabels", expectedJson));
    }
}


