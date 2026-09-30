package ru.practicum.shareit.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemRequestDto {
    private Long id;

    private String description;

    private Long requestorId;

    private LocalDateTime created;

    private List<ItemAnswerDto> items;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ItemAnswerDto {
        private Long id;
        private String name;
        private Long ownerId;
    }
}
