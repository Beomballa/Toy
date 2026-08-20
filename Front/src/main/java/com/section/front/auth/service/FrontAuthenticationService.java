package com.section.front.auth.service;

import com.section.common.system.entity.Account;
import com.section.common.system.repository.AccountRepository;
import com.section.front.auth.dto.FrontMemberPasswordChangeRequest;
import com.section.front.auth.dto.FrontMemberProfileUpdateRequest;
import com.section.front.auth.dto.FrontMemberSignUpRequest;
import com.section.front.auth.support.FrontMemberSession.AuthenticatedFrontMember;
import com.section.front.auth.support.FrontPasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FrontAuthenticationService {

    private final AccountRepository accountRepository;
    private final FrontPasswordEncoder passwordEncoder;

    @Transactional
    public AuthenticatedFrontMember signUp(FrontMemberSignUpRequest request) {
        String email = normalizeEmail(request.email());
        if (accountRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        }

        Account account = Account.createCustomer(
                email,
                passwordEncoder.encode(request.password()),
                normalizeRequiredText(request.name(), "이름"),
                normalizeOptionalText(request.nickname())
        );
        try {
            return authenticated(accountRepository.saveAndFlush(account));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.", exception);
        }
    }

    @Transactional
    public Optional<AuthenticatedFrontMember> authenticate(String rawEmail, String rawPassword) {
        String email = normalizeEmail(rawEmail);
        Optional<Account> found = accountRepository.findByEmailIgnoreCase(email);
        if (found.isEmpty()) {
            passwordEncoder.consumeDummyMatch(rawPassword);
            return Optional.empty();
        }

        Account account = found.get();
        boolean passwordMatches = passwordEncoder.matches(rawPassword, account.getPassword());
        if (!account.isAvailableCustomer() || !passwordMatches) {
            return Optional.empty();
        }
        if (passwordEncoder.needsRehash(account.getPassword())) {
            account.changePassword(passwordEncoder.encode(rawPassword));
        }
        return Optional.of(authenticated(account));
    }

    @Transactional
    public AuthenticatedFrontMember changePassword(long memberId, FrontMemberPasswordChangeRequest request) {
        Account account = accountRepository.findByIdForUpdate(memberId)
                .filter(Account::isAvailableCustomer)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보를 다시 확인해 주세요."));

        if (!passwordEncoder.matches(request.currentPassword(), account.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 비밀번호를 확인해 주세요.");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "새 비밀번호는 현재 비밀번호와 다르게 입력해 주세요.");
        }

        account.changePassword(passwordEncoder.encode(request.newPassword()));
        return authenticated(account);
    }

    @Transactional
    public AuthenticatedFrontMember updateProfile(long memberId, FrontMemberProfileUpdateRequest request) {
        Account account = accountRepository.findByIdForUpdate(memberId)
                .filter(Account::isAvailableCustomer)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보를 다시 확인해 주세요."));
        account.setName(normalizeRequiredText(request.name(), "이름"));
        account.setNickname(normalizeOptionalText(request.nickname()));
        return authenticated(account);
    }

    private AuthenticatedFrontMember authenticated(Account account) {
        return new AuthenticatedFrontMember(
                account.getId(),
                account.getEmail(),
                account.getName(),
                account.getNickname() == null ? "" : account.getNickname()
        );
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("이메일을 입력하세요.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRequiredText(String value, String fieldName) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + "을 입력하세요.");
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }
}
