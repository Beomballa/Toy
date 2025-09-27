package com.section.common.system.service;

import com.section.common.system.entity.Account;
import com.section.common.system.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAccountService {

    private final AccountRepository accountRepository;

    public Optional<Account> findAccountInfo(String email, String password) {
        if(StringUtils.hasText(email) && StringUtils.hasText(password)){
            return accountRepository.findByAdminInfo(email,  password);
        }else {
            return Optional.empty();
        }
    }
}
