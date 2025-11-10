package com.dashboard.service;

import com.dashboard.dto.UpdateResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {
    @Autowired
    private ObjectMapper objectMapper;

    private List<SseEmitter> sseEmitters = new CopyOnWriteArrayList<>();

    public SseEmitter addEmitter() {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);

        sseEmitters.add(emitter);
        System.out.println("새 클라이언트 연결됨: " + emitter);

        emitter.onCompletion(() -> {
            sseEmitters.remove(emitter);
            System.out.println("클라이언트 연결 끊김: " + emitter);
        });

        emitter.onTimeout(() -> {
            sseEmitters.remove(emitter);
            System.out.println("클라이언트 시간초과: " + emitter);
        });

        emitter.onError((error) -> {
            sseEmitters.remove(emitter);
            System.out.println("클라이언트 오류: " + emitter);
        });

        try {
            emitter.send(SseEmitter.event().name("connect").data("SSE 연결 설정됨"));
        } catch (IOException e) {
            System.out.println("초기 connect 메시지 전송 실패 (무시함): " + e.getMessage());
        }

        return emitter;
    }


    public void sendUpdateNotification(UpdateResponseDTO data) {
        String jsonData;

        try {
            jsonData = objectMapper.writeValueAsString(data);
        } catch (IOException e) {
            System.out.println("SSE JSON 변환 오류:" + e.getMessage());
            return;
        }

        sseEmitters.forEach(emitter->{
            try{
                emitter.send(SseEmitter.event().name("update").data(jsonData));
            }catch (IOException e){
                System.out.println("SSE 이벤트 보내기 실패: " + e.getMessage());
            }
        });
    }
}
