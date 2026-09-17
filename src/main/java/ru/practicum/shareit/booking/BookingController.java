package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;

import java.util.List;

@RestController
@RequestMapping(path = "/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @PostMapping
    public BookingResponseDto createBooking(@RequestHeader(USER_ID_HEADER) Long bookerId,
                                            @Valid @RequestBody BookingRequestDto bookingRequestDto) {
        log.info("Запрос POST /bookings на создание бронирования от пользователя: {}", bookerId);
        return bookingService.create(bookerId, bookingRequestDto);
    }

    @PatchMapping("/{bookingId}")
    public BookingResponseDto approveBooking(@RequestHeader(USER_ID_HEADER) Long ownerId,
                                             @PathVariable Long bookingId,
                                             @RequestParam Boolean approved) {
        log.info("Запрос PATCH /bookings/{} от владельца {} со статусом approved={}", bookingId, ownerId, approved);
        return bookingService.approve(ownerId, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public BookingResponseDto getBookingById(@RequestHeader(USER_ID_HEADER) Long userId,
                                             @PathVariable Long bookingId) {
        log.info("Запрос GET /bookings/{} от пользователя {}", bookingId, userId);
        return bookingService.getById(userId, bookingId);
    }

    @GetMapping
    public List<BookingResponseDto> getAllByBooker(@RequestHeader(USER_ID_HEADER) Long bookerId,
                                                   @RequestParam(defaultValue = "ALL") String state) {
        log.info("Запрос GET /bookings от арендатора {} с параметром state={}", bookerId, state);
        return bookingService.getAllByBooker(bookerId, state);
    }

    @GetMapping("/owner")
    public List<BookingResponseDto> getAllByOwner(@RequestHeader(USER_ID_HEADER) Long ownerId,
                                                  @RequestParam(defaultValue = "ALL") String state) {
        log.info("Запрос GET /bookings/owner от владельца {} с параметром state={}", ownerId, state);
        return bookingService.getAllByOwner(ownerId, state);
    }
}
