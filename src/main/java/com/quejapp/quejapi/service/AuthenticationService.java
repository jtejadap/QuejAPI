package com.quejapp.quejapi.service;

import java.util.HashMap;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.quejapp.quejapi.dto.AuthenticationRequest;
import com.quejapp.quejapi.dto.AuthenticationResponse;
import com.quejapp.quejapi.dto.RegisterRequest;
import com.quejapp.quejapi.dto.UserSession;
import com.quejapp.quejapi.model.Role;
import com.quejapp.quejapi.model.User;
import com.quejapp.quejapi.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest registerRequest, Role role) {
        var user = User.builder()
                .firstname(registerRequest.getFirstname())
                .lastname(registerRequest.getLastname())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .birthdate(registerRequest.getBirthdate())
                .gender(registerRequest.getGender())
                .role(role)
                .build();
        userRepository.save(user);
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefresh(new HashMap<>(), user);
        return AuthenticationResponse.builder()
                .authenticationToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authenticationRequest.getEmail(), authenticationRequest.getPassword())
        );
        var user = userRepository.findByEmail(authenticationRequest.getEmail()).orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefresh(new HashMap<>(), user);

        var UserSessionData = UserSession.builder()
                .name(user.getFirstname() + " " + user.getLastname())
                .roles(new String[]{user.getRole().name()})
                .expiration(jwtService.getExpirationDateFromToken(jwtToken))
                .build();

        return AuthenticationResponse.builder()
                .authenticationToken(jwtToken)
                .refreshToken(refreshToken)
                .userData(UserSessionData)              
                .build();
    }

    public AuthenticationResponse refreshToken(String refreshToken) {

        var user = userRepository.findByEmail(jwtService.getEmailFromToken(refreshToken)).orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        var jwtToken = jwtService.generateToken(user);
        var newRefreshToken = jwtService.generateRefresh(new HashMap<>(), user);
        return AuthenticationResponse.builder()
                .authenticationToken(jwtToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    public Boolean validateToken(String token) {
        return jwtService.validateToken(token);
    }
}
