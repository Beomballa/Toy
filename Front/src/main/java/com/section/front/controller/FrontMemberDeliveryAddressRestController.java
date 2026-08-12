package com.section.front.controller;
import com.section.front.auth.support.FrontMemberSession;
import com.section.front.memberaddress.dto.FrontDeliveryAddressResponse;
import com.section.front.memberaddress.service.FrontMemberDeliveryAddressService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import com.section.front.memberaddress.dto.FrontDeliveryAddressRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
@RestController @RequiredArgsConstructor @RequestMapping("/api/front/member/delivery-addresses")
public class FrontMemberDeliveryAddressRestController {
 private final FrontMemberDeliveryAddressService addressService;
 @GetMapping public List<FrontDeliveryAddressResponse> getAddresses(HttpServletRequest request){ var member=FrontMemberSession.read(request.getSession(false)); if(member==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"로그인이 필요합니다."); return addressService.getAddresses(member.memberId()); }
 @PostMapping public List<FrontDeliveryAddressResponse> save(@Valid @RequestBody FrontDeliveryAddressRequest body,HttpServletRequest r){return addressService.save(memberNo(r),body);}
 @PutMapping("/{id}") public List<FrontDeliveryAddressResponse> update(@PathVariable long id,@Valid @RequestBody FrontDeliveryAddressRequest body,HttpServletRequest r){return addressService.update(memberNo(r),id,body);}
 @PutMapping("/{id}/default") public List<FrontDeliveryAddressResponse> setDefault(@PathVariable long id,HttpServletRequest r){return addressService.setDefault(memberNo(r),id);}
 @DeleteMapping("/{id}") public List<FrontDeliveryAddressResponse> delete(@PathVariable long id,HttpServletRequest r){return addressService.delete(memberNo(r),id);}
 private long memberNo(HttpServletRequest r){var m=FrontMemberSession.read(r.getSession(false));if(m==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"로그인이 필요합니다.");return m.memberId();}
}
