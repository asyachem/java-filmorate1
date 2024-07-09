package ru.yandex.practicum.filmorate.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    private final Map<Long, User> users = new HashMap<>();
    private final static Logger log = LoggerFactory.getLogger(UserController.class);

    @GetMapping
    public Collection<User> findAll() {
        return users.values();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        if (user.getEmail() == null || user.getEmail().isBlank() || user.getEmail().indexOf('@') == -1) {
            log.warn("ошибка при вводе почты");
            throw new ValidationException("Почта не может быть пустым полем и должна содержать @");
        }
        if (user.getLogin() == null || user.getLogin().indexOf(' ') != -1) {
            log.warn("ошибка при вводе логина");
            throw new ValidationException("Логин не может быть пустым и содержать пробелы");
        }
        if (user.getName() == null) {
            user.setName(user.getLogin());
        }
        if (user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("ошибка при вводе даты рождения");
            throw new ValidationException("Дата рождения не может быть в будущем");
        }

        checkEmail(user);

        user.setId(getNextId());
        users.put(user.getId(), user);
        return user;
    }

    @PutMapping
    public User update(@RequestBody User newUser) {
        if (newUser.getId() == null) {
            log.warn("ошибка - не указан id");
            throw new ConditionsNotMetException("Id должен быть указан");
        }
        if (users.containsKey(newUser.getId())) {
            User oldUser = users.get(newUser.getId());

            if (!oldUser.getEmail().equals(newUser.getEmail())) {
                checkEmail(newUser);
            }

            if (newUser.getName() != null) {
                oldUser.setName(newUser.getName());
            }
            if (newUser.getEmail() != null) {
                if (newUser.getEmail().indexOf('@') == -1) {
                    log.warn("емейл указан без @");
                    throw new ValidationException("Почта должна содержать @");
                }
                oldUser.setEmail(newUser.getEmail());
            }
            if (newUser.getLogin() != null) {
                if (newUser.getLogin().indexOf(' ') != -1) {
                    log.warn("логин указан с пробелами");
                    throw new ValidationException("Логин не может быть пустым и содержать пробелы");
                }
                oldUser.setLogin(newUser.getLogin());
            }
            if (newUser.getBirthday() != null) {
                if (newUser.getBirthday().isAfter(LocalDate.now())) {
                    log.warn("указали неверную дату рождения");
                    throw new ValidationException("Дата рождения не может быть в будущем");
                }
                oldUser.setBirthday(newUser.getBirthday());
            }

            return oldUser;
        }
        log.warn("пользователь с заданным айди не был обнаружен");
        throw new NotFoundException("Пользователь с id = " + newUser.getId() + " не найден");
    }

    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    private void checkEmail(User user) {
        for (Map.Entry<Long, User> entry : users.entrySet()) {
            if(user.getEmail().equals(entry.getValue().getEmail())) {
                log.warn("уже существует пользователь с заданным айди");
                throw new DuplicatedDataException("Этот емейл уже используется");
            }
        }
    }
}
