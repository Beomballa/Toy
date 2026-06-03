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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertEquals(0, response.currentPage());
        assertEquals(20, response.pageSize());
        assertEquals("전체 1명", response.resultMeta().resultLabel());
        assertEquals("1-1 / 1명 · 1페이지", response.resultMeta().pageInfoLabel());
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
        account.setProfileImgPath("/images/profiles/");
        account.setProfileImgName("user-3.png");

        when(accountRepository.findById(3L)).thenReturn(Optional.of(account));

        AdminMemberDetailResponse response = adminMemberService.getMemberDetail(3L);

        assertEquals("user@test.com", response.email());
        assertEquals("Y", response.masterYn());
        assertEquals("/images/profiles/user-3.png", response.profileImgPath());
    }

    @Test
    @DisplayName("회원 상세는 파일명이 없는 프로필 디렉터리 경로를 이미지 경로로 노출하지 않는다")
    void getMemberDetailIgnoresDirectoryOnlyProfilePath() {
        Account account = new Account();
        account.setId(4L);
        account.setEmail("user2@test.com");
        account.setProfileImgPath("/images/profiles/");

        when(accountRepository.findById(4L)).thenReturn(Optional.of(account));

        AdminMemberDetailResponse response = adminMemberService.getMemberDetail(4L);

        assertNull(response.profileImgPath());
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

    @Test
    @DisplayName("회원 CSV 내보내기는 현재 필터와 회원 행을 기록한다")
    void exportMemberListCsvIncludesSummaryAndRows() {
        AdminMemberListRequest request = new AdminMemberListRequest();
        request.setKeyword("member");
        request.setMasterYn("Y");

        AccountListResDto row = new AccountListResDto();
        row.setId(1L);
        row.setEmail("member@test.com");
        row.setName("회원");
        row.setNickname("닉네임");
        row.setMasterYn(YN.Y);
        row.setInitYn(YN.N);
        row.setDelYn(YN.N);

        when(accountRepository.getAccountList(any(), any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 1000), 1));

        String csv = new String(adminMemberService.exportMemberListCsv(request), UTF_8);

        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("최신 가입순 · 검색=member · 권한=Y"));
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("\"1\",\"회원\",\"닉네임\",\"member@test.com\",\"마스터\",\"정상\",\"정상\""));
    }
}
