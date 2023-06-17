package com.example.Project.Dlook.service;

import com.example.Project.Dlook.domain.BlackList;
import com.example.Project.Dlook.domain.Member;
import com.example.Project.Dlook.domain.RefreshToken;
import com.example.Project.Dlook.domain.dto.JoinRequestDTO;
import com.example.Project.Dlook.domain.dto.LoginRequestDTO;
import com.example.Project.Dlook.domain.dto.TokenDto;
import com.example.Project.Dlook.domain.dto.TokenRequestDto;
import com.example.Project.Dlook.exception.AppException;
import com.example.Project.Dlook.exception.ErrorCode;
import com.example.Project.Dlook.repository.BlackListRepository;
import com.example.Project.Dlook.repository.MemberRepository;
import com.example.Project.Dlook.repository.RefreshTokenRepository;
import com.example.Project.Dlook.utils.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BlackListRepository blackListRepository;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtProvider jwtProvider;
    private final BCryptPasswordEncoder encoder;

    @Transactional
    public ResponseEntity<String> join(JoinRequestDTO dto) {

        // memberName 중복 체크
        memberRepository.findByMemberName(dto.getMemberName())
                .ifPresent(member -> {
                    throw new AppException(ErrorCode.MEMBERNAME_DUPLICATED, dto.getMemberName() + " already Exist");
                });

        // memberEmail 중복 체크
        memberRepository.findByMemberEmail(dto.getMemberEmail())
                .ifPresent(member -> {
                    throw new AppException(ErrorCode.MEMBEREMAIL_DUPLICATED, dto.getMemberEmail() + " already Exist");
                });

        Member member = dto.toMember(encoder);
        memberRepository.save(member);

        return ResponseEntity.ok().body("login success");
    }

    @Transactional
    public ResponseEntity<String> login(LoginRequestDTO dto, HttpServletResponse response) {
        // memberEmail 없음
        Member selectedMemberEmail = memberRepository.findByMemberEmail(dto.getMemberEmail())
                .orElseThrow(() -> new AppException(ErrorCode.MEMBEREMAIL_NOT_FOUND, dto.getMemberEmail() + " MEMBEREMAIL_NOT_FOUND"));

        // memberPw 틀림 ( memberRepository에 있는 pw와 들어온 pw 비교)
        // log.info("selectedPw:{} pw:{}", selectedMemberEmail.getMemberPw(), memberPw);
        if (!encoder.matches(dto.getMemberPw(), selectedMemberEmail.getMemberPw())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD, " INVALID_PASSWORD");
        }

        UsernamePasswordAuthenticationToken authenticationToken = dto.toAuthentication();

        // 2. 실제로 검증 (사용자 비밀번호 체크) 이 이루어지는 부분
        // authenticate 메서드가 실행이 될 때 CustomUserDetailsService 에서 만들었던 loadUserByUsername 메서드가 실행됨
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        TokenDto tokenDto = jwtProvider.generateTokenDto(authentication);
        response.addHeader("Authorization", "Bearer " + tokenDto.getAccessToken());
        response.addHeader("RefreshToken", tokenDto.getRefreshToken());
        response.addHeader("MemberName", authentication.getName());

        RefreshToken refreshToken = RefreshToken.builder()
                .refreshKey(authentication.getName())
                .refreshValue(tokenDto.getRefreshToken())
                .build();
        refreshTokenRepository.save(refreshToken);

        return ResponseEntity.ok().body("login success");
    }

    @Transactional
    public ResponseEntity<String> reissue(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = request.getHeader("Authorization").substring(7);
        String refreshToken = request.getHeader("RefreshToken");

        // 1. Refresh Token 검증 (accesstoken이 만료된 이후라 인증없이 항상 permit 해둠, logout 이후의 접근을 막기 위해 필요)
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid Refresh Token");
        }

        // 2. Access Token 에서 Member ID 가져오기
        Authentication authentication = jwtProvider.getAuthentication(accessToken);

        // 3. 저장소에서 Member ID 를 기반으로 Refresh Token 값 가져옴
        RefreshToken refreshTokenValue = refreshTokenRepository.findByRefreshKey(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Member Logout"));

        // 4. Refresh Token 일치하는지 검사
        if (!refreshTokenValue.getRefreshValue().equals(refreshToken)) {
            throw new RuntimeException("REFRESHTOKEN_NOT_FOUND");
        }

        // 5. 새로운 토큰 생성
        TokenDto tokenDto = jwtProvider.generateTokenDto(authentication);
        response.addHeader("Authorization", "Bearer " + accessToken);
        response.addHeader("RefreshToken", refreshToken);
        response.addHeader("MemberName", authentication.getName());

        // 6. 저장소 정보 업데이트
        RefreshToken newRefreshToken = refreshTokenValue.updateValue(refreshToken);
        refreshTokenRepository.save(newRefreshToken);

        // 토큰 발급
        return ResponseEntity.ok().body("Token reissue");
    }

    public ResponseEntity<String> logout(HttpServletRequest request) {
        String accessToken = request.getHeader("Authorization").substring(7);

        // 1. accessToken 검증
        if (!jwtProvider.validateToken(accessToken)) {
            throw new RuntimeException("Invalid accessToken");
        }

        // 2. Access Token 에서 Member ID 가져오기
        Authentication authentication = jwtProvider.getAuthentication(accessToken);

        // 3. 저장소에서 Member ID 를 기반으로 Refresh Token 값 가져옴
        RefreshToken refreshTokenValue = refreshTokenRepository.findByRefreshKey(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Member Logout"));
        if (refreshTokenValue != null) {
            refreshTokenRepository.delete(refreshTokenValue);
        }

        Long expiration = jwtProvider.getExpiration(accessToken);
        BlackList blackList = BlackList.builder()
                .accessToken(accessToken)
                .message("logout")
                .expiration(expiration)
                .build();
        blackListRepository.save(blackList);

        return ResponseEntity.ok().body("logout success");
    }
}
