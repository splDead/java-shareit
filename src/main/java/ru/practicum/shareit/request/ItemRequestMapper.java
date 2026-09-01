package ru.practicum.shareit.request;

import ru.practicum.shareit.request.dto.ItemRequestDto;

public final class ItemRequestMapper {

    private ItemRequestMapper() { }

    public static ItemRequestDto toItemRequestDto(ItemRequest itemRequest) {
        if (itemRequest == null) {
            return null;
        }

        return ItemRequestDto.builder()
            .id(itemRequest.getId())
            .description(itemRequest.getDescription())
            .requestorId(itemRequest.getRequestorId())
            .created(itemRequest.getCreated())
            .build();
    }

    public static ItemRequest toItemRequest(ItemRequestDto itemRequestDto) {
        if (itemRequestDto == null) {
            return null;
        }

        return ItemRequest.builder()
            .id(itemRequestDto.getId())
            .description(itemRequestDto.getDescription())
            .requestorId(itemRequestDto.getRequestorId())
            .created(itemRequestDto.getCreated())
            .build();
    }
}
