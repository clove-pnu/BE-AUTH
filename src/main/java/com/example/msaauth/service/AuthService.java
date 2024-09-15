package com.example.msaauth.service;



import com.example.msaauth.controller.AuthController;
import com.example.msaauth.dto.*;
import com.example.msaauth.entity.Member;
import com.example.msaauth.entity.RefreshToken;
import com.example.msaauth.exception.DuplicateEmailException;
import com.example.msaauth.jwt.TokenProvider;
import com.example.msaauth.repository.MemberRepository;
import com.example.msaauth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    @Transactional
    public MemberResponseDto signup(MemberRequestDto memberRequestDto) {
        if (memberRepository.existsByEmail(memberRequestDto.getEmail())) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
        }

        Member member = memberRequestDto.toMember(passwordEncoder);
        Member savedMember = memberRepository.save(member);

        // 이메일과 권한 정보만 응답
        return MemberResponseDto.of(savedMember);
    }

    @Transactional
    public TokenResponseDto login(MemberRequestDto memberRequestDto) {
        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    memberRequestDto.toAuthentication();

            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

            TokenDto tokenDto = tokenProvider.generateTokenDto(authentication);

            RefreshToken refreshToken = RefreshToken.builder()
                    .key(authentication.getName())
                    .value(tokenDto.getRefreshToken())
                    .memberId(Long.parseLong(authentication.getName()))
                    .build();

            refreshTokenRepository.save(refreshToken);

            // 액세스 토큰과 리프레시 토큰 정보 반환
            return new TokenResponseDto(tokenDto.getAccessToken(), tokenDto.getRefreshToken());
        } catch (BadCredentialsException e) {
            throw new RuntimeException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }
    }

    @Transactional
    public void logout(MemberRequestDto memberRequestDto) {
        // 사용자 인증
        Member member = memberRepository.findByEmail(memberRequestDto.getEmail())
                .orElseThrow(() -> new RuntimeException("가입되지 않은 이메일입니다."));

        if (!passwordEncoder.matches(memberRequestDto.getPassword(), member.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }
        Long memberId = member.getId();
        RefreshToken refreshToken = refreshTokenRepository.findByMemberId(memberId)
                .orElseThrow(()-> new RuntimeException("로그인 되어 있지 않은 사용자입니다."));

        // RefreshToken 삭제
        refreshTokenRepository.delete(refreshToken);

        // SecurityContextHolder에서 현재 Authentication 제거
        SecurityContextHolder.clearContext();
    }

    @Transactional
    public TokenDto reissue(String refreshToken) {
        // 1. Refresh Token 검증
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Refresh Token이 유효하지 않습니다.");
        }
        logger.info("서비스1");
        // 2. Refresh Token에서 Member ID 가져오기
        Authentication authentication = tokenProvider.getAuthenticationFromRefreshToken(refreshToken);
        logger.info("서비스2");

        // 3. 저장소에서 Member ID 를 기반으로 Refresh Token 값 가져옴
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByKey(authentication.getName())
                .orElseThrow(() -> new RuntimeException("로그아웃 된 사용자입니다."));
        logger.info("서비스3");

        // 4. Refresh Token 일치하는지 검사
        if (!refreshTokenEntity.getValue().equals(refreshToken)) {
            throw new RuntimeException("토큰의 유저 정보가 일치하지 않습니다.");
        }
        logger.info("서비스4");

        // 5. 새로운 토큰 생성
        TokenDto tokenDto = tokenProvider.generateTokenDto(authentication);

        // 6. 저장소 정보 업데이트
        RefreshToken newRefreshToken = refreshTokenEntity.updateValue(tokenDto.getRefreshToken());
        refreshTokenRepository.save(newRefreshToken);

        // 토큰 발급
        return tokenDto;
    }
}