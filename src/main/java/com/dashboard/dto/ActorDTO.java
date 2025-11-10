package com.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActorDTO {
    private String login;
    @JsonProperty("avatar_url")
    private String avatarUrl;
    private int count;
}
