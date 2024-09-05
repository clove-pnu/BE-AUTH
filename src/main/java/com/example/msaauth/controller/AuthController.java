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
    public ResponseEntity<MemberResponseDto> signup(@RequestBody MemberRequestDto memberRequestDto) {
        return ResponseEntity.ok(authService.signup(memberRequestDto));
    }
    @PostMapping("/login")
    public ResponseEntity<TokenDto> login(@RequestBody MemberRequestDto memberRequestDto, HttpServletResponse response) {
        TokenDto tokenDto = authService.login(memberRequestDto);

        // Refresh 토큰을 쿠키에 추가
        CookieUtil.addRefreshTokenToCookie(response, tokenDto.getRefreshToken());

        // Access 토큰만 포함된 새로운 TokenDto 생성
        TokenDto responseTokenDto = TokenDto.builder()
                .grantType(tokenDto.getGrantType())
                .accessToken(tokenDto.getAccessToken())
                .accessTokenExpiresIn(tokenDto.getAccessTokenExpiresIn())
                .build();

        return ResponseEntity.ok(responseTokenDto);
    }
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody MemberRequestDto memberRequestDto) {
        authService.logout(memberRequestDto);
        String responseMessage = memberRequestDto.getEmail() + " logout 완료";
        return ResponseEntity.ok(responseMessage);
    }
    @PostMapping("/reissue")
    public ResponseEntity<TokenDto> reissue(HttpServletRequest request, HttpServletResponse response) {


        // 쿠키에서 Refresh 토큰 추출
        String refreshToken = CookieUtil.extractRefreshToken(request)
                .orElseThrow(() -> new RuntimeException("Refresh Token이 존재하지 않습니다."));
        logger.info("컨트롤러0");

        TokenDto tokenDto = authService.reissue(refreshToken);
        logger.info("컨트롤러1");
        // 새로운 Refresh 토큰을 쿠키에 추가
        CookieUtil.addRefreshTokenToCookie(response, tokenDto.getRefreshToken());

        // Access 토큰만 포함된 새로운 TokenDto 생성
        TokenDto responseTokenDto = TokenDto.builder()
                .grantType(tokenDto.getGrantType())
                .accessToken(tokenDto.getAccessToken())
                .accessTokenExpiresIn(tokenDto.getAccessTokenExpiresIn())
                .build();

        return ResponseEntity.ok(responseTokenDto);
    }

}