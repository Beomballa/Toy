package com.section.admin.content.req;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentListReqDto {

    @NotEmpty
    @Pattern(regexp = "EN|KO")
    private String langCode="KO";

    @NotEmpty
    @Pattern(regexp = "\\d*")
    private String pageKO="1";

    @NotEmpty
    @Pattern(regexp = "\\d*")
    private String pageEN="1";

    private String searchKeyword;
}
