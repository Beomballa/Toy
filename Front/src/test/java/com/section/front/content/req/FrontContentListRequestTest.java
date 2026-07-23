package com.section.front.content.req;

import com.section.common.content.dto.DocumentListSort;
import com.section.common.content.entity.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FrontContentListRequestTest {

    @Test
    @DisplayName("콘텐츠 검색 조건은 공백과 대소문자를 정규화한다")
    void normalizesSearchConditions() {
        FrontContentListRequest request =
                new FrontContentListRequest(" style ", "  여름 스타일  ", 2, 12, " popular ");

        assertThat(request.normalizedBoardType()).isEqualTo(Document.BoardType.STYLE);
        assertThat(request.normalizedKeyword()).isEqualTo("여름 스타일");
        assertThat(request.pageable().getPageNumber()).isEqualTo(2);
        assertThat(request.pageable().getPageSize()).isEqualTo(12);
        assertThat(request.normalizedSort()).isEqualTo(DocumentListSort.POPULAR);
    }

    @Test
    @DisplayName("전체 조건과 빈 검색어는 동적 조건에서 제외한다")
    void removesAllAndBlankConditions() {
        FrontContentListRequest request = new FrontContentListRequest("ALL", "  ", null, null);

        assertThat(request.normalizedBoardType()).isNull();
        assertThat(request.normalizedKeyword()).isNull();
        assertThat(request.pageable().getPageNumber()).isZero();
        assertThat(request.pageable().getPageSize()).isEqualTo(8);
        assertThat(request.normalizedSort()).isEqualTo(DocumentListSort.LATEST);
    }

    @Test
    @DisplayName("프론트에 노출하지 않는 게시판 유형은 거부한다")
    void rejectsUnsupportedBoardType() {
        FrontContentListRequest request = new FrontContentListRequest("QNA", null, null, null);

        assertThatThrownBy(request::normalizedBoardType).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("검색어와 페이지 범위를 벗어난 요청은 거부한다")
    void rejectsInvalidPagingAndKeyword() {
        assertThatThrownBy(() -> new FrontContentListRequest(null, "a".repeat(101), null, null).normalizedKeyword())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FrontContentListRequest(null, null, -1, 8).pageable())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FrontContentListRequest(null, null, 0, 21).pageable())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FrontContentListRequest(null, null, 0, 8, "RANDOM").normalizedSort())
                .isInstanceOf(IllegalArgumentException.class);
    }
}
