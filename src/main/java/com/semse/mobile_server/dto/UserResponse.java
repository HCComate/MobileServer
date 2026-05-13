package com.semse.mobile_server.dto;

import com.semse.mobile_server.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserResponse {
    private String userId;
    private String name;
    private String role;
    private String shiftStatus;
    private String workStatus;
    private List<String> assignedDevices;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .role(user.getRole())
                .shiftStatus(user.getShiftStatus())
                .workStatus(user.getWorkStatus())
                .assignedDevices(user.getAssignedDevices())
                .build();
    }
}