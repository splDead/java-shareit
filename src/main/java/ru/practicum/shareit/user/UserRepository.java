package ru.practicum.shareit.user;

import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class UserRepository {
    private final Map<Long, User> users = new HashMap<>();
    private long currentId = 1;

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(currentId++);
        }
        users.put(user.getId(), user);
        return user;
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    public void deleteById(Long id) {
        users.remove(id);
    }

    public boolean existsById(Long id) {
        return users.containsKey(id);
    }

    public boolean existsByEmail(String email) {
        return users.values().stream()
            .anyMatch(user -> user.getEmail().equalsIgnoreCase(email));
    }
}
