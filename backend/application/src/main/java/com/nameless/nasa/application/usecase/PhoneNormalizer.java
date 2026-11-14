package com.nameless.nasa.application.usecase;

final class PhoneNormalizer {
    private PhoneNormalizer() {
    }

    static String normalize(String phone) {
        return phone.replaceAll("[\\s\\-()]", "");
    }
}
