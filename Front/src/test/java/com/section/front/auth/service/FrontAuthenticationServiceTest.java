package com.section.front.auth.service;

import com.section.common.base.entity.type.YN;
import com.section.common.system.entity.Account;
import com.section.common.system.repository.AccountRepository;
import com.section.front.auth.dto.FrontMemberSignUpRequest;
import com.section.front.auth.support.FrontPasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FrontAuthenticationServiceTest {

    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final FrontPasswordEncoder passwordEncoder = new FrontPasswordEncoder();
    private FrontAuthenticationService service;

    @BeforeEach
    void setUp() {
        service = new FrontAuthenticationService(accountRepository, passwordEncoder);
    }

    @Test
    void signsUpNormalizedCustomerWithEncodedPassword() {
        given(accountRepository.existsByEmailIgnoreCase("member@example.com")).willReturn(false);
        given(accountRepository.saveAndFlush(any(Account.class))).willAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(10L);
            return account;
        });

        var member = service.signUp(new FrontMemberSignUpRequest(
                " Member@Example.com ", "noren1234", " 홍 길동 ", " 길동 "
        ));

        assertThat(member.memberId()).isEqualTo(10L);
        assertThat(member.email()).isEqualTo("member@example.com");
        assertThat(member.name()).isEqualTo("홍 길동");
        verify(accountRepository).saveAndFlush(any(Account.class));
    }

    @Test
    void rejectsDuplicatedEmailBeforeEncodingAccount() {
        given(accountRepository.existsByEmailIgnoreCase("member@example.com")).willReturn(true);

        assertThatThrownBy(() -> service.signUp(new FrontMemberSignUpRequest(
                "member@example.com", "noren1234", "회원", null
        )))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    @Test
    void authenticatesLegacyPasswordAndRehashesIt() {
        Account account = Account.createCustomer("member@example.com", "legacy1234", "회원", null);
        account.setId(11L);
        given(accountRepository.findByEmailIgnoreCase("member@example.com")).willReturn(Optional.of(account));

        assertThat(service.authenticate("MEMBER@example.com", "legacy1234")).isPresent();
        assertThat(account.getPassword()).startsWith("{pbkdf2}");
    }

    @Test
    void rejectsDeletedAccountEvenWithMatchingPassword() {
        Account account = Account.createCustomer("member@example.com", passwordEncoder.encode("noren1234"), "회원", null);
        account.setId(12L);
        account.setDelYn(YN.Y);
        given(accountRepository.findByEmailIgnoreCase("member@example.com")).willReturn(Optional.of(account));

        assertThat(service.authenticate("member@example.com", "noren1234")).isEmpty();
    }
}
