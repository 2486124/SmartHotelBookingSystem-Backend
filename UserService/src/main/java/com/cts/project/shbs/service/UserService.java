package com.cts.project.shbs.service;

import java.util.List;

import com.cts.project.shbs.dto.JwtResponse;
import com.cts.project.shbs.dto.LoginRequest;
import com.cts.project.shbs.dto.RegisterRequest;
import com.cts.project.shbs.model.User;

public interface UserService {
    User registerUser(RegisterRequest request);
    JwtResponse loginUser(LoginRequest request);
    User getUserById(Long id);
    List<User> getAllUsers();
    void deleteUser(Long id);
    User updateUserProfile(Long id, RegisterRequest request);
    List<User> searchUsers(String keyword);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
}