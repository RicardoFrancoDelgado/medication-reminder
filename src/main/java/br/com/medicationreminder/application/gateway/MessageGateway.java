package br.com.medicationreminder.application.gateway;

public interface MessageGateway {
    void sendMessage(String to, String message);
}
