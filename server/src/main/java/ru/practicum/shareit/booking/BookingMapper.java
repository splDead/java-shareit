package ru.practicum.shareit.booking;

import ru.practicum.shareit.booking.dto.BookingResponseDto;

public final class BookingMapper {

    private BookingMapper() {
    }

    public static BookingResponseDto toBookingResponseDto(Booking booking) {
        if (booking == null) {
            return null;
        }

        return BookingResponseDto.builder()
            .id(booking.getId())
            .start(booking.getStart())
            .end(booking.getEnd())
            .status(booking.getStatus())
            .booker(new BookingResponseDto.BookerDto(booking.getBooker().getId()))
            .item(new BookingResponseDto.ItemDto(booking.getItem().getId(), booking.getItem().getName()))
            .build();
    }
}
