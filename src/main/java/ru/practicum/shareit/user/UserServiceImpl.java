package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.user.dto.UserDto;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserDto createUser(UserDto userDto) {
        log.info("Создание пользователя: {}", userDto.getEmail());

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }

        User user = UserMapper.toUser(userDto);

        return UserMapper.toUserDto(userRepository.save(user));
    }

    @Override
    public UserDto updateUser(Long userId, UserDto userDto) {
        log.info("Обновление пользователя с id: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("Пользователь с id " + userId + " не найден"));

        if (userDto.getEmail() != null && !userDto.getEmail().isBlank()) {
            if (!user.getEmail().equalsIgnoreCase(userDto.getEmail())
                    && userRepository.existsByEmail(userDto.getEmail())) {
                throw new ConflictException("Пользователь с таким email уже существует");
            }

            user.setEmail(userDto.getEmail());
        }

        if (userDto.getName() != null && !userDto.getName().isBlank()) {
            user.setName(userDto.getName());
        }

        return UserMapper.toUserDto(userRepository.save(user));
    }

    @Override
    public UserDto getUserById(Long userId) {
        return userRepository.findById(userId)
            .map(UserMapper::toUserDto)
            .orElseThrow(() -> new NoSuchElementException("Пользователь с id " + userId + " не найден"));
    }

    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
            .map(UserMapper::toUserDto)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NoSuchElementException("Пользователь с id " + userId + " не найден");
        }

        userRepository.deleteById(userId);
    }
}
