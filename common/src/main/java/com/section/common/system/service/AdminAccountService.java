package com.section.common.system.service;

import com.section.common.system.entity.Account;
import com.section.common.system.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAccountService {

    private final AccountRepository accountRepository;

    public Account findAccountInfo(String email, String password) {
        if(!StringUtils.hasText(email) && !StringUtils.hasText(password)){
            return accountRepository.findByAdminInfo(email,  password);
        }
        return null;
    }
}
