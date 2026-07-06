package com.section.admin.user.service;

import com.section.admin.user.req.AdminMemberListRequest;
import com.section.admin.user.req.AdminMemberBulkStatusUpdateRequest;
import com.section.admin.user.req.AdminMemberStatusUpdateRequest;
import com.section.admin.user.res.AdminMemberDetailResponse;
import com.section.admin.user.res.AdminMemberListResponse;
import com.section.admin.user.res.AdminMemberSummaryResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.base.entity.type.YN;
import com.section.common.system.dto.AccountListResDto;
import com.section.common.system.dto.AccountSummaryDto;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @DisplayName("회원 요약은 현재 필터 기준 카운트를 반환한다")
    void getMemberSummaryReturnsSummaryResponse() {
        AdminMemberListRequest request = new AdminMemberListRequest();
        request.setKeyword("member");

        when(accountRepository.getAccountSummary(any()))
                .thenReturn(new AccountSummaryDto(12, 2, 10, 3, 4));

        AdminMemberSummaryResponse response = adminMemberService.getMemberSummary(request);

        assertEquals(12, response.totalCount());
        assertEquals(2, response.masterCount());
        assertEquals(10, response.normalCount());
        assertEquals(3, response.deletedCount());
        assertEquals(4, response.tempPasswordCount());
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
        account.setTmpPwIssueDt(java.time.LocalDateTime.of(2026, 6, 11, 9, 30));
        account.setProfileImgPath("/images/profiles/");
        account.setProfileImgName("user-3.png");

        when(accountRepository.findById(3L)).thenReturn(Optional.of(account));

        AdminMemberDetailResponse response = adminMemberService.getMemberDetail(3L);

        assertEquals("user@test.com", response.email());
        assertEquals("Y", response.masterYn());
        assertEquals("2026-06-11 09:30", response.tmpPwIssueDtm());
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
    @DisplayName("회원 상세는 확장자 없이 점만 포함한 잘못된 프로필 경로를 이미지 경로로 노출하지 않는다")
    void getMemberDetailIgnoresMalformedProfilePath() {
        Account account = new Account();
        account.setId(5L);
        account.setEmail("user3@test.com");
        account.setProfileImgPath("/images/profiles.");

        when(accountRepository.findById(5L)).thenReturn(Optional.of(account));

        AdminMemberDetailResponse response = adminMemberService.getMemberDetail(5L);

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
    @DisplayName("회원 일괄 상태 변경은 선택한 회원의 권한과 상태를 함께 갱신한다")
    void updateMemberStatusesUpdatesFlags() {
        Account first = new Account();
        first.setId(4L);
        first.setMasterYn(YN.N);
        first.setDelYn(YN.N);

        Account second = new Account();
        second.setId(5L);
        second.setMasterYn(YN.Y);
        second.setDelYn(YN.N);

        when(accountRepository.findAllById(List.of(4L, 5L)))
                .thenReturn(List.of(first, second));

        AdminMemberService.BulkStatusUpdateResult result = adminMemberService.updateMemberStatuses(
                new AdminMemberBulkStatusUpdateRequest(List.of(4L, 5L), true, true)
        );

        assertEquals(2, result.requestedCount());
        assertEquals(2, result.updatedCount());
        assertEquals(0, result.unchangedCount());
        assertEquals(YN.Y, first.getMasterYn());
        assertEquals(YN.Y, first.getDelYn());
        assertEquals(YN.Y, second.getMasterYn());
        assertEquals(YN.Y, second.getDelYn());
    }

    @Test
    @DisplayName("회원 일괄 상태 변경은 변경 항목이 없으면 INVALID_INPUT_VALUE 예외를 던진다")
    void updateMemberStatusesThrowsWhenNoChangesProvided() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                adminMemberService.updateMemberStatuses(new AdminMemberBulkStatusUpdateRequest(List.of(4L), null, null))
        );

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("회원 CSV 내보내기는 현재 필터와 회원 행을 기록한다")
    void exportMemberListCsvIncludesSummaryAndRows() {
        AdminMemberListRequest request = new AdminMemberListRequest();
        request.setKeyword("member");
        request.setMasterYn("Y");
        request.setInitYn("Y");

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

        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("최신 가입순 · 검색=member · 권한=Y · 비밀번호=임시비밀번호"));
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("\"1\",\"회원\",\"닉네임\",\"member@test.com\",\"마스터\",\"정상\",\"정상\""));
    }
}
