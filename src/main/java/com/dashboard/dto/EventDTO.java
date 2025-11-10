package com.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDTO {
    private String id;
    private ActorDTO actor;
    private String type;
    private RepoDTO repo;
    @JsonProperty("created_at")
    private String createdAt;


}
