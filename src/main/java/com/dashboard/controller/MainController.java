package com.dashboard.controller;

import com.dashboard.dto.*;
import com.dashboard.service.DataReprocessService;
import com.dashboard.service.GithubApiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class MainController {

    @Autowired
    private GithubApiService githubApiService;
    @Autowired
    private DataReprocessService dataReprocessService;
    @Autowired
    private ObjectMapper objectMapper;


    private final int PAGE_SIZE = 10;
    private final int PAGE_BLOCK_SIZE = 5;


    @GetMapping("/")
    public String showDashboard(
            Model model,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest
    ) {
        //총 갯수 조회
        int totalCount = dataReprocessService.getTotalEventCount();

        //현재 리스트 조회
        List<EventDTO> eventList = dataReprocessService.findEventsForPage(page, PAGE_SIZE);

        //통계 조회
        List<EventStatsDTO> eventStats = dataReprocessService.getEventStats();


        //데이터 합치기
        PaginationDTO pageData = new PaginationDTO(totalCount, page, PAGE_SIZE, PAGE_BLOCK_SIZE, eventList);

        model.addAttribute("pageData", pageData);
        model.addAttribute("eventStats", eventStats);

        //차트 로직
        try {
            //라벨
            List<String> chartLabels = eventStats.stream()
                    .map(EventStatsDTO::getType)
                    .toList();

            //데이터
            List<Integer> chartData = eventStats.stream()
                    .map(EventStatsDTO::getCount)
                    .toList();

            //JSON 변환
            model.addAttribute("chartLabels", objectMapper.writeValueAsString(chartLabels));
            model.addAttribute("chartData", objectMapper.writeValueAsString(chartData));


        } catch (JsonProcessingException e) {
            System.out.println("예외 발생: " + e.getMessage());

            model.addAttribute("chartLabels", "[]");
            model.addAttribute("chartData", "[]");
        }

        List<RepoDTO> topRepos = dataReprocessService.getTopRepos();
        List<ActorDTO> topUsers = dataReprocessService.getTopUsers();

        model.addAttribute("topRepos", topRepos);
        model.addAttribute("topUsers", topUsers);


        if (hxRequest != null) {
            return "page/main :: list-fragment";
        }

        return "page/main";
    }


}
