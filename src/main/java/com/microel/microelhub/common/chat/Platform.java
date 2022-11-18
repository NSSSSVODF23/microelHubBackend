package com.microel.microelhub.common.chat;

public enum Platform {
    VK, TELEGRAM, WHATSAPP, INTERNAL;

    public String getLocalized() {
        switch (ordinal()) {
            case 0:
                return "Вконтакте";
            case 1:
                return "Телеграм";
            case 2:
                return "WhatsApp";
            case 3:
                return "Vdonsk.ru";
            default:
                return "";
        }
    }
}
