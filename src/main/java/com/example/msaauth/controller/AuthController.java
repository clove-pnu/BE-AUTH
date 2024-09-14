package com.example.msaauth.controller;

import com.example.msaauth.dto.MemberRequestDto;
import com.example.msaauth.dto.MemberResponseDto;
import com.example.msaauth.dto.TokenDto;
import com.example.msaauth.service.AuthService;
import com.example.msaauth.util.CookieUtil;
import com.mysql.cj.log.Log;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody MemberRequestDto memberRequestDto) {
        String responseMessage = memberRequestDto.getEmail()+", "+memberRequestDto.getAuthority() + " 회원가입 완료";
        return ResponseEntity.ok(responseMessage);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody MemberRequestDto memberRequestDto, HttpServletResponse response) {
        TokenDto tokenDto = authService.login(memberRequestDto);

        // Access 토큰을 Authorization 헤더에 저장
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDto.getAccessToken());

        // Refresh 토큰을 HttpOnly 쿠키에 저장
        CookieUtil.addRefreshTokenToCookie(response, tokenDto.getRefreshToken());

        String successMessage = memberRequestDto.getEmail() + " 로그인 성공";

        return ResponseEntity.ok().headers(headers).body(successMessage);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody MemberRequestDto memberRequestDto) {
        authService.logout(memberRequestDto);
        String responseMessage = memberRequestDto.getEmail() + " logout 완료";
        return ResponseEntity.ok(responseMessage);
    }

    @PostMapping("/reissue")
    public ResponseEntity<String> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = CookieUtil.extractTokenFromCookie(request, "refreshToken")
                .orElseThrow(() -> new RuntimeException("Refresh Token이 존재하지 않습니다."));

        TokenDto tokenDto = authService.reissue(refreshToken);

        // 새로운 Access 토큰을 Authorization 헤더에 저장
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDto.getAccessToken());

        // Refresh 토큰을 HttpOnly 쿠키에 저장
        CookieUtil.addRefreshTokenToCookie(response, tokenDto.getRefreshToken());

        String successMessage = "Access,Refresh 토큰이 정상적으로 발급되었습니다.";

        return ResponseEntity.ok().headers(headers).body(successMessage);
    }
}

