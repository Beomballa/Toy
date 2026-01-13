package com.section.admin.content.req;

import com.section.admin.base.req.PagingReqDto;
import com.section.common.content.dto.ContentListItemDto;
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

    public ContentListItemDto toContentListItemDto(Account account) {
        ContentListItemDto dto = new ContentListItemDto();
        dto.setSearchKeyword(this.searchKeyword == null ? "" : this.searchKeyword);
        dto.setSearchKeywordType(this.searchKeywordType == null ? "" : this.searchKeywordType);
        dto.setAdminNo(account.getCrtNo());
        return dto;
    }
}
