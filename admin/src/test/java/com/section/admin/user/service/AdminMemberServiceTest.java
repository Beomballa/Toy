package com.section.admin.user.service;

import com.section.admin.user.req.AdminMemberListRequest;
import com.section.admin.user.req.AdminMemberStatusUpdateRequest;
import com.section.admin.user.res.AdminMemberDetailResponse;
import com.section.admin.user.res.AdminMemberListResponse;
import com.section.common.base.entity.type.YN;
import com.section.common.system.dto.AccountListResDto;
import com.section.common.system.entity.Account;
import com.section.common.system.repository.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMemberServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AdminMemberService adminMemberService;

    @Test
    @DisplayName("회원 목록은 필터 결과를 운영용 DTO로 반환한다")
    void getMemberListReturnsPagedResponse() {
        AccountListResDto row = new AccountListResDto();
        row.setId(1L);
        row.setEmail("member@test.com");
        row.setName("회원");
        row.setNickname("닉네임");
        row.setMasterYn(YN.N);
        row.setDelYn(YN.N);

        when(accountRepository.getAccountList(any(), any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1));

        AdminMemberListResponse response = adminMemberService.getMemberList(new AdminMemberListRequest(), PageRequest.of(0, 20));

        assertEquals(1, response.items().size());
        assertEquals("member@test.com", response.items().get(0).email());
    }

    @Test
    @DisplayName("회원 상세는 엔티티를 운영용 응답으로 변환한다")
    void getMemberDetailReturnsResponse() {
        Account account = new Account();
        account.setId(3L);
        account.setEmail("user@test.com");
        account.setName("사용자");
        account.setNickname("유저");
        account.setMasterYn(YN.Y);
        account.setDelYn(YN.N);

        when(accountRepository.findById(3L)).thenReturn(Optional.of(account));

        AdminMemberDetailResponse response = adminMemberService.getMemberDetail(3L);

        assertEquals("user@test.com", response.email());
        assertEquals("Y", response.masterYn());
    }

    @Test
    @DisplayName("회원 상태 변경은 권한과 탈퇴 여부를 함께 갱신한다")
    void updateMemberStatusUpdatesFlags() {
        Account account = new Account();
        account.setId(4L);
        account.setMasterYn(YN.N);
        account.setDelYn(YN.N);

        when(accountRepository.findById(4L)).thenReturn(Optional.of(account));

        adminMemberService.updateMemberStatus(4L, new AdminMemberStatusUpdateRequest(true, true));

        assertEquals(YN.Y, account.getMasterYn());
        assertEquals(YN.Y, account.getDelYn());
    }
}
