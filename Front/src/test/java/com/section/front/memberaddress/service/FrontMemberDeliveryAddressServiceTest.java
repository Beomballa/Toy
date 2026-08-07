package com.section.front.memberaddress.service;

import com.section.common.commerce.entity.FrontMemberDeliveryAddress;
import com.section.common.commerce.repository.FrontMemberDeliveryAddressRepository;
import com.section.common.system.entity.Account;
import com.section.common.system.repository.AccountRepository;
import com.section.front.memberaddress.dto.FrontDeliveryAddressRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FrontMemberDeliveryAddressServiceTest {
    private final FrontMemberDeliveryAddressRepository addressRepository = mock(FrontMemberDeliveryAddressRepository.class);
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final FrontMemberDeliveryAddressService service = new FrontMemberDeliveryAddressService(addressRepository, accountRepository);

    @Test
    void replacesExistingDefaultWhenSavingANewDefaultAddress() {
        Account account = mock(Account.class);
        FrontMemberDeliveryAddress existing = mock(FrontMemberDeliveryAddress.class);
        given(account.isAvailableCustomer()).willReturn(true);
        given(accountRepository.findById(7L)).willReturn(Optional.of(account));
        given(addressRepository.findAllByMemberNoOrderByDefaultYnDescIdDesc(7L))
                .willReturn(List.of(existing), List.of());

        service.save(7L, request(true));

        verify(existing).setDefault(false);
        verify(addressRepository).save(any(FrontMemberDeliveryAddress.class));
    }

    @Test
    void promotesNextAddressWhenDeletingTheDefaultAddress() {
        Account account = mock(Account.class);
        FrontMemberDeliveryAddress defaultAddress = mock(FrontMemberDeliveryAddress.class);
        FrontMemberDeliveryAddress nextAddress = mock(FrontMemberDeliveryAddress.class);
        given(account.isAvailableCustomer()).willReturn(true);
        given(accountRepository.findById(7L)).willReturn(Optional.of(account));
        given(defaultAddress.isDefaultAddress()).willReturn(true);
        given(addressRepository.findByIdAndMemberNo(10L, 7L)).willReturn(Optional.of(defaultAddress));
        given(addressRepository.findAllByMemberNoOrderByDefaultYnDescIdDesc(7L))
                .willReturn(List.of(nextAddress), List.of());

        service.delete(7L, 10L);

        verify(addressRepository).delete(defaultAddress);
        verify(nextAddress).setDefault(true);
    }

    private FrontDeliveryAddressRequest request(boolean defaultAddress) {
        return new FrontDeliveryAddressRequest("집", "홍길동", "010-1111-2222", "06236", "서울시 강남구", "101호", defaultAddress);
    }
}
