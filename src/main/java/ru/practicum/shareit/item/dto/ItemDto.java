package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.practicum.shareit.user.dto.CreateGroup;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemDto {
    private Long id;

    @NotBlank(message = "Название вещи не может быть пустым", groups = CreateGroup.class)
    private String name;

    @NotBlank(message = "Описание вещи не может быть пустым", groups = CreateGroup.class)
    private String description;

    @NotNull(message = "Статус доступности должен быть указан", groups = CreateGroup.class)
    private Boolean available;

    private Long requestId;

    private BookingInfo lastBooking;
    private BookingInfo nextBooking;
    private List<CommentResponseDto> comments;

    @Data
    @AllArgsConstructor
    public static class BookingInfo {
        private Long id;
        private Long bookerId;
    }
}
