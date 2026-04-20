package com.section.common.system.service;

import com.section.common.system.entity.Account;
import com.section.common.system.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAccountService {
    private final AccountRepository accountRepository;

    public Page<Account> getAccountList(Pageable pageable) {
        return accountRepository.findAll(pageable);
    }
}
