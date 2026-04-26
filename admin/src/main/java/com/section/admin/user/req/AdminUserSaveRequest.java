package com.section.admin.user.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserSaveRequest(
        Long adminNo,

        @NotBlank(message = "로그인 ID는 필수입니다.")
        @Size(max = 50, message = "로그인 ID는 50자 이하여야 합니다.")
        String loginId,

        @Size(max = 255, message = "비밀번호는 255자 이하여야 합니다.")
        String password,

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @NotBlank(message = "권한은 필수입니다.")
        String role,

        @NotBlank(message = "상태는 필수입니다.")
        String status
) {
    public boolean isNewAdmin() {
        return adminNo == null;
    }
}
