package ru.practicum.shareit.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {

    private Long id;

    @NotBlank(message = "Имя пользователя не может быть пустым", groups = CreateGroup.class)
    private String name;

    @NotBlank(message = "Email не может быть пустым", groups = CreateGroup.class)
    @Email(message = "Некорректный формат email", groups = {CreateGroup.class, UpdateGroup.class})
    private String email;
}
