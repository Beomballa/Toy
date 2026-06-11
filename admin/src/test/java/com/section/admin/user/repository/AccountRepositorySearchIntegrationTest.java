package com.section.admin.user.repository;

import com.section.admin.AdminToyApplication;
import com.section.common.base.entity.type.YN;
import com.section.common.system.dto.AccountListQuery;
import com.section.common.system.dto.AccountSummaryDto;
import com.section.common.system.entity.Account;
import com.section.common.system.repository.AccountRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = AdminToyApplication.class)
@ActiveProfiles("local")
@Transactional
class AccountRepositorySearchIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("회원 목록 검색은 공백으로 구분한 여러 키워드를 모두 만족하는 회원만 조회한다")
    void getAccountListMatchesAllKeywordTokens() {
        accountRepository.save(account("member@test.com", "배송 담당", "재고 점검", YN.N, YN.N, YN.N));
        accountRepository.save(account("member@test.com", "배송팀", "일반 회원", YN.N, YN.N, YN.N));
        accountRepository.save(account("guest@test.com", "재고 담당", "점검 요원", YN.N, YN.N, YN.N));
        entityManager.flush();
        entityManager.clear();

        Page<?> result = accountRepository.getAccountList(
                new AccountListQuery("배송 점검", null, null, null),
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("회원 요약은 현재 필터 기준 전체, 권한, 탈퇴, 초기 비밀번호 수를 집계한다")
    void getAccountSummaryAggregatesFilteredCounts() {
        accountRepository.save(account("master1@test.com", "회원통계전용 마스터 회원", "primary", YN.Y, YN.N, YN.N));
        accountRepository.save(account("master2@test.com", "회원통계전용 마스터 탈퇴", "inactive", YN.Y, YN.Y, YN.Y));
        accountRepository.save(account("member1@test.com", "회원통계전용 일반 회원", "starter", YN.N, YN.Y, YN.N));
        accountRepository.save(account("member2@test.com", "회원통계전용 일반 탈퇴", "former", YN.N, YN.N, YN.Y));
        entityManager.flush();
        entityManager.clear();

        AccountSummaryDto summary = accountRepository.getAccountSummary(new AccountListQuery("회원통계전용", null, null, null));

        assertEquals(4, summary.totalCount());
        assertEquals(2, summary.masterCount());
        assertEquals(2, summary.normalCount());
        assertEquals(2, summary.deletedCount());
        assertEquals(2, summary.tempPasswordCount());
    }

    @Test
    @DisplayName("회원 목록과 요약은 임시 비밀번호 필터를 함께 적용한다")
    void getAccountListAndSummaryApplyInitYnFilter() {
        accountRepository.save(account("temp1@test.com", "회원초기비밀번호전용 임시 회원", "reset", YN.N, YN.Y, YN.N));
        accountRepository.save(account("temp2@test.com", "회원초기비밀번호전용 정상 회원", "stable", YN.N, YN.N, YN.N));
        entityManager.flush();
        entityManager.clear();

        Page<?> list = accountRepository.getAccountList(
                new AccountListQuery("회원초기비밀번호전용", null, null, YN.Y),
                PageRequest.of(0, 10)
        );
        AccountSummaryDto summary = accountRepository.getAccountSummary(new AccountListQuery("회원초기비밀번호전용", null, null, YN.Y));

        assertEquals(1, list.getTotalElements());
        assertEquals(1, summary.totalCount());
        assertEquals(1, summary.tempPasswordCount());
    }

    private Account account(String email, String name, String nickname, YN masterYn, YN initYn, YN delYn) {
        Account account = new Account();
        account.setEmail(email);
        account.setPassword("pw");
        account.setName(name);
        account.setNickname(nickname);
        account.setMasterYn(masterYn);
        account.setInitYn(initYn);
        account.setDelYn(delYn);
        return account;
    }
}
