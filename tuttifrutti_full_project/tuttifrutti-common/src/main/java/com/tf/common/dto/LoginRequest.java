
package com.tf.common.dto;

public class LoginRequest {
    public String action = "LOGIN";
    public String playerName;

    public LoginRequest() {}

    public LoginRequest(String playerName) {
        this.playerName = playerName;
    }
}
