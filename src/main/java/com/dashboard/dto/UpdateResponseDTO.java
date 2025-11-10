package com.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateResponseDTO {
    private List<RepoDTO> topRepos;
    private List<ActorDTO> topUsers;
    private List<EventStatsDTO> eventStats;
    private List<EventDTO> eventList;
    private List<String> chartLabels;
    private List<Integer> chartData;
    private int totalCount;
}
