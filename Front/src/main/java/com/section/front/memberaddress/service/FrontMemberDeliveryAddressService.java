package com.section.front.memberaddress.service;
import com.section.common.commerce.entity.FrontMemberDeliveryAddress;
import com.section.common.commerce.repository.FrontMemberDeliveryAddressRepository;
import com.section.common.system.repository.AccountRepository;
import com.section.front.memberaddress.dto.FrontDeliveryAddressResponse;
import com.section.front.memberaddress.dto.FrontDeliveryAddressRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
@Service @RequiredArgsConstructor
public class FrontMemberDeliveryAddressService {
    private final FrontMemberDeliveryAddressRepository addressRepository; private final AccountRepository accountRepository;
    @Transactional(readOnly=true) public List<FrontDeliveryAddressResponse> getAddresses(long memberNo){ requireMember(memberNo); return addresses(memberNo); }
    @Transactional public List<FrontDeliveryAddressResponse> save(long memberNo, FrontDeliveryAddressRequest request){ requireMember(memberNo); var current=addressRepository.findAllByMemberNoOrderByDefaultYnDescIdDesc(memberNo); boolean primary=request.defaultAddress()||current.isEmpty(); if(primary) current.forEach(a->a.setDefault(false)); addressRepository.save(FrontMemberDeliveryAddress.create(memberNo,text(request.addressName()),text(request.recipientName()),phone(request.recipientPhone()),text(request.postalCode()),text(request.address1()),blankToNull(request.address2()),primary)); return addresses(memberNo); }
    @Transactional public List<FrontDeliveryAddressResponse> setDefault(long memberNo,long id){ requireMember(memberNo); var current=addressRepository.findAllByMemberNoOrderByDefaultYnDescIdDesc(memberNo); var selected=current.stream().filter(a->a.getId().equals(id)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"배송지를 찾을 수 없습니다.")); current.forEach(a->a.setDefault(a==selected)); return addresses(memberNo); }
    @Transactional public List<FrontDeliveryAddressResponse> delete(long memberNo,long id){ requireMember(memberNo); var address=addressRepository.findByIdAndMemberNo(id,memberNo).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"배송지를 찾을 수 없습니다.")); boolean wasDefault=address.isDefaultAddress(); addressRepository.delete(address); var remaining=addressRepository.findAllByMemberNoOrderByDefaultYnDescIdDesc(memberNo); if(wasDefault&&!remaining.isEmpty()) remaining.get(0).setDefault(true); return addresses(memberNo); }
    private List<FrontDeliveryAddressResponse> addresses(long memberNo){return addressRepository.findAllByMemberNoOrderByDefaultYnDescIdDesc(memberNo).stream().map(FrontDeliveryAddressResponse::from).toList();}
    private String text(String v){String n=v==null?"":v.trim().replaceAll("\\s+"," ");if(n.isBlank())throw new IllegalArgumentException("배송지 정보가 올바르지 않습니다.");return n;}
    private String blankToNull(String v){return v==null||v.isBlank()?null:text(v);}
    private String phone(String v){String n=v==null?"":v.replaceAll("[^0-9]","");if(!n.matches("\\d{10,11}"))throw new IllegalArgumentException("연락처 정보가 올바르지 않습니다.");return n;}
    private void requireMember(long memberNo){ if(accountRepository.findById(memberNo).filter(a->a.isAvailableCustomer()).isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"사용할 수 없는 회원입니다."); }
}
