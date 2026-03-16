package com.section.admin.content.req;

import com.section.admin.base.req.PagingReqDto;
import com.section.common.system.entity.Account;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentListReqDto extends PagingReqDto {

    @NotEmpty
    @Pattern(regexp = "EN|KO")
    private String langCode="KO";

    private String searchKeyword;

    // T : 제목,  C : 내용
    private String searchKeywordType;
}
