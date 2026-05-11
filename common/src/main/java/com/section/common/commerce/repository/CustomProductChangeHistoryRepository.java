package com.section.common.commerce.repository;

import com.section.common.commerce.dto.ProductHistoryListQuery;
import com.section.common.commerce.dto.ProductHistoryListResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomProductChangeHistoryRepository {

    Page<ProductHistoryListResDto> getProductHistoryList(ProductHistoryListQuery query, Pageable pageable);
}
