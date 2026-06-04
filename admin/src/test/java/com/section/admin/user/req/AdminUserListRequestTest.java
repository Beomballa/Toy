package com.section.admin.user.req;

import com.section.common.base.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminUserListRequestTest {

    @Test
    @DisplayName("관리자 목록 요청은 검색어와 필터를 정규화한다")
    void toQueryNormalizesValues() {
        AdminUserListRequest request = new AdminUserListRequest();
        request.setKeyword("  master   ops ");
        request.setRole(" role_super ");
        request.setStatus(" active ");
        request.setInactiveDays(14);
        request.setNeverLoggedInOnly("y");

        var query = request.toQuery();

        assertEquals("master ops", query.keyword());
        assertEquals("ROLE_SUPER", query.role());
        assertEquals("ACTIVE", query.status());
        assertEquals(14, query.inactiveDays());
        assertEquals(Boolean.TRUE, query.neverLoggedInOnly());
    }

    @Test
    @DisplayName("관리자 목록 요청은 잘못된 권한 값을 거부한다")
    void toQueryRejectsInvalidRole() {
        AdminUserListRequest request = new AdminUserListRequest();
        request.setRole("OWNER");

        assertThrows(BusinessException.class, request::toQuery);
    }

    @Test
    @DisplayName("관리자 목록 요청은 0 이하 미접속 일수를 거부한다")
    void toQueryRejectsInvalidInactiveDays() {
        AdminUserListRequest request = new AdminUserListRequest();
        request.setInactiveDays(0);

        assertThrows(BusinessException.class, request::toQuery);
    }

    @Test
    @DisplayName("관리자 목록 요청은 로그인 이력 없음 필터를 정규화한다")
    void toQueryNormalizesNeverLoggedInOnly() {
        AdminUserListRequest request = new AdminUserListRequest();
        request.setNeverLoggedInOnly("y");

        var query = request.toQuery();

        assertEquals(Boolean.TRUE, query.neverLoggedInOnly());
    }
}
