package com.dashboard.mapper;

import com.dashboard.dto.ActorDTO;
import com.dashboard.dto.EventDTO;
import com.dashboard.dto.EventStatsDTO;
import com.dashboard.dto.RepoDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EventMapper {
    
    //이벤트들 찾기
    List<EventDTO> findAllEventsPaginated(
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    //게시물 총 갯수
    int getTotalCount();

    //이벤트 통계
    List<EventStatsDTO> getEventStats();
    
    //이벤트들 DB에 넣기
    void insertEventList(List<EventDTO> eventList);
    
    //7일이상 된 것들 지우기
    void deleteOldEvents();

    //활동량 많은 리포지토리
    List<RepoDTO> getTopRepos(@Param("limit") int limit);

    //활동량 많은 사용자
    List<ActorDTO> getTopUsers(@Param("limit") int limit);
}
