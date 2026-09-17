package ru.practicum.shareit.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingRequestDto {

    @NotNull(message = "ID вещи не может быть пустым")
    private Long itemId;

    @NotNull(message = "Дата начала бронирования не может быть пустой")
    @FutureOrPresent(message = "Дата начала бронирования не может быть в прошлом")
    private LocalDateTime start;

    @NotNull(message = "Дата окончания бронирования не может быть пустой")
    @Future(message = "Дата окончания бронирования должна быть в будущем")
    private LocalDateTime end;
}
