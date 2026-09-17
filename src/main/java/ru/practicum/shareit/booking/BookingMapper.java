package ru.practicum.shareit.booking;

import ru.practicum.shareit.booking.dto.BookingDto;

public final class BookingMapper {

    private BookingMapper() {

    }

    public static BookingDto toBookingDto(Booking booking) {
        if (booking == null) {
            return null;
        }
        return BookingDto.builder()
            .id(booking.getId())
            .start(booking.getStart())
            .end(booking.getEnd())
            .itemId(booking.getItemId())
            .bookerId(booking.getBookerId())
            .status(booking.getStatus())
            .build();
    }

    public static Booking toBooking(BookingDto bookingDto) {
        if (bookingDto == null) {
            return null;
        }
        return Booking.builder()
            .id(bookingDto.getId())
            .start(bookingDto.getStart())
            .end(bookingDto.getEnd())
            .itemId(bookingDto.getItemId())
            .bookerId(bookingDto.getBookerId())
            .status(bookingDto.getStatus())
            .build();
    }
}
