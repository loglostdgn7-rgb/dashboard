package com.dashboard.service;

import com.dashboard.dto.*;
import com.dashboard.mapper.EventMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataReprocessService {
    @Autowired
    private EventMapper eventMapper;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CacheManager cacheManager;

    private final int TOP_LIST_LIMIT = 3;


   public List<EventDTO> findEventsForPage(int currentPage, int pageSize){
       int offset = (currentPage - 1) * pageSize;


       return  eventMapper.findAllEventsPaginated(pageSize, offset);
   }


    @Cacheable("totalEventCount")
    public int getTotalEventCount() {
        System.out.println("getTotalCount() 실행");

        return eventMapper.getTotalCount();
    }



    @Cacheable("eventStats")
    public List<EventStatsDTO> getEventStats() {
        System.out.println("getEventStats 실행, DB 조회");

        return eventMapper.getEventStats();
    }


    @Cacheable("topRepos")
    public List<RepoDTO> getTopRepos() {
        System.out.println("getTopRepos 실행, DB 조회");
        return eventMapper.getTopRepos(TOP_LIST_LIMIT);
    }

    @Cacheable("topUsers")
    public List<ActorDTO> getTopUsers() {
        System.out.println("getTopUsers 실행, DB 조회");
        return eventMapper.getTopUsers(TOP_LIST_LIMIT);
    }



    //스케쥴러용
    public int getTotalEventCount_Fresh() {
        System.out.println("getTotalCount_Fresh() 실행 (DB 직접)");
        return eventMapper.getTotalCount();
    }

    public List<EventStatsDTO> getEventStats_Fresh() {
        System.out.println("getEventStats_Fresh 실행 (DB 직접)");
        return eventMapper.getEventStats();
    }

    public List<RepoDTO> getTopRepos_Fresh() {
        System.out.println("getTopRepos_Fresh 실행 (DB 직접)");
        return eventMapper.getTopRepos(TOP_LIST_LIMIT);
    }

    public List<ActorDTO> getTopUsers_Fresh() {
        System.out.println("getTopUsers_Fresh 실행 (DB 직접)");
        return eventMapper.getTopUsers(TOP_LIST_LIMIT);
    }



}
