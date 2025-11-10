package com.dashboard.service;

import com.dashboard.dto.EventDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Service
public class GithubApiService {

    @Value("${github.token}")
    private String GITHUB_TOKEN;

//    @Autowired
//    RestTemplate restTemplate;

    @Autowired
    private WebClient webClient;

    private final String GITHUB_API_URL = "https://api.github.com/";


    public List<EventDTO> getGithubActivity() {


        //RestTemplate 쓰던거
//        HttpHeaders headers = new HttpHeaders();
//        headers.add("Authorization", "Bearer " + GITHUB_TOKEN);
//        headers.set("Accept", "application/vnd.github.v3+json");
//        HttpEntity<String> entity = new HttpEntity<>(headers);
//        String apiUrl = GITHUB_API_URL + "/events";
//
//        try {
//            ResponseEntity<EventDTO[]> response = restTemplate.exchange(
//                    apiUrl,
//                    HttpMethod.GET,
//                    entity,
//                    EventDTO[].class
//            );
//
//            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
//                return Arrays.asList(response.getBody());
//            } else {
//                System.out.println("Error: " + response.getStatusCode());
//                return null;
//            }


        //WebClient
        try{
            return webClient.get()
                    .uri(GITHUB_API_URL +"/events")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + GITHUB_TOKEN)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                    .retrieve()
                    .onStatus(httpStatus -> httpStatus.is4xxClientError() || httpStatus.is5xxServerError(),
                            clientResponse -> Mono.error(new RuntimeException("요청 에러: " + clientResponse.statusCode())))
                    .bodyToFlux(EventDTO.class)
                    .collectList()
                    .block();

        } catch (Exception e) {
            System.out.println("API 호출중 오류 발생: " + e.getMessage());
            return null;
        }
    }

}
