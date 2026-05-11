package com.section.common.system.repository;

import com.section.common.system.dto.AccountListQuery;
import com.section.common.system.dto.AccountListResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomAccountRepository {

    Page<AccountListResDto> getAccountList(AccountListQuery query, Pageable pageable);
}
