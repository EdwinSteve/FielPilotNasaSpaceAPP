package com.nameless.nasa.domain.command;

public record LoginCommand(String phone, String rawPassword) {
}
