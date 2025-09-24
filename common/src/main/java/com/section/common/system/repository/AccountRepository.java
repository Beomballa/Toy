package com.section.common.system.repository;

import com.section.common.system.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account,Long> {

    @Query("SELECT a FROM Account a where a.email =:email and a.password =:password")
    Account findByAdminInfo(String email, String password);
}
