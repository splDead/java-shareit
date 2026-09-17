package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dto.BookingDto;
import java.util.List;

@RestController
@RequestMapping(path = "/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @PostMapping
    public BookingDto createBooking(@RequestHeader(USER_ID_HEADER) Long bookerId,
                                    @RequestBody BookingDto bookingDto) {
        log.info("Запрос POST /bookings на создание бронирования от пользователя: {}", bookerId);

        bookingDto.setBookerId(bookerId);
        bookingDto.setStatus(BookingStatus.WAITING);

        return bookingDto;
    }

    @PatchMapping("/{bookingId}")
    public BookingDto approveBooking(@RequestHeader(USER_ID_HEADER) Long ownerId,
                                     @PathVariable Long bookingId,
                                     @RequestParam Boolean approved) {
        log.info("Запрос PATCH /bookings/{} от владельца {} со статусом approved={}", bookingId, ownerId, approved);

        return BookingDto.builder()
            .id(bookingId)
            .status(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED)
            .build();
    }

    @GetMapping("/{bookingId}")
    public BookingDto getBookingById(@RequestHeader(USER_ID_HEADER) Long userId,
                                     @PathVariable Long bookingId) {
        log.info("Запрос GET /bookings/{} от пользователя {}", bookingId, userId);

        return BookingDto.builder().id(bookingId).build();
    }

    @GetMapping
    public List<BookingDto> getAllByBooker(@RequestHeader(USER_ID_HEADER) Long bookerId,
                                           @RequestParam(defaultValue = "ALL") String state) {
        log.info("Запрос GET /bookings от арендатора {} с параметром state={}", bookerId, state);

        return List.of();
    }

    @GetMapping("/owner")
    public List<BookingDto> getAllByOwner(@RequestHeader(USER_ID_HEADER) Long ownerId,
                                          @RequestParam(defaultValue = "ALL") String state) {
        log.info("Запрос GET /bookings/owner от владельца {} с параметром state={}", ownerId, state);

        return List.of();
    }
}
