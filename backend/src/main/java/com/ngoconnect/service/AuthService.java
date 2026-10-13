package com.ngoconnect.service;

import com.ngoconnect.dto.AuthRequest;
import com.ngoconnect.dto.AuthResponse;
import com.ngoconnect.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(AuthRequest request);
    AuthResponse adminLogin(AuthRequest request);
}
