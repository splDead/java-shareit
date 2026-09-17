package ru.practicum.shareit.request;

import ru.practicum.shareit.request.dto.ItemRequestDto;
import java.util.ArrayList;

public final class ItemRequestMapper {

    private ItemRequestMapper() {
    }

    public static ItemRequestDto toItemRequestDto(ItemRequest request) {
        if (request == null) {
            return null;
        }
        return ItemRequestDto.builder()
            .id(request.getId())
            .description(request.getDescription())
            .requestorId(request.getRequestor().getId())
            .created(request.getCreated())
            .items(new ArrayList<>())
            .build();
    }

    public static ItemRequest toItemRequest(ItemRequestDto dto) {
        if (dto == null) {
            return null;
        }
        return ItemRequest.builder()
            .id(dto.getId())
            .description(dto.getDescription())
            .created(dto.getCreated())
            .build();
    }
}
