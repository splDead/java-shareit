package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.practicum.shareit.user.dto.CreateGroup;

@Data
public class ItemDto {

    @NotBlank(message = "Название вещи не может быть пустым", groups = CreateGroup.class)
    private String name;

    @NotBlank(message = "Описание вещи не может быть пустым", groups = CreateGroup.class)
    private String description;

    @NotNull(message = "Статус доступности должен быть указан", groups = CreateGroup.class)
    private Boolean available;

    private Long requestId;
}
