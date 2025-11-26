
package com.tf.common.dto;

public class LoginResponse {
    public boolean success;
    public String message;

    public LoginResponse() {}

    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
