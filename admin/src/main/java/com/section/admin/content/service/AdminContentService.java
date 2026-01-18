package com.section.admin.content.service;

import com.section.admin.content.req.ContentListReqDto;
import com.section.admin.content.req.ContentSetReqDto;
import com.section.admin.content.req.UpdateViewCountReqDto;
import com.section.admin.content.res.ContentGetResDto;
import com.section.admin.content.res.ContentMyDocResDto;
import com.section.admin.content.res.CreateDocumentDefaultInfoResDto;
import com.section.common.content.dto.DocumentListItemDto;
import com.section.common.system.entity.Account;
import com.section.common.system.service.AdminAccountService;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import com.section.common.content.service.DocumentService;
import com.section.common.system.entity.ApprovalDocument;
import com.section.common.system.repository.ApprovalDocumentRepository;
import com.section.common.system.service.ApprovalDocumentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminContentService {

    private final ApprovalDocumentService approvalDocumentService;
    private final DocumentService documentService;
    private final AdminAccountService adminAccountService;

    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final DocumentRepository documentRepository;

    public ContentMyDocResDto listDocument(ContentListReqDto reqDto) {
        try{
            Account currentAccount = adminAccountService.findAccountInfo("wjdqjatnwkd@gmail.com", "1234")
                    .orElseThrow(() -> new EntityNotFoundException("계정 정보를 찾을 수 없습니다."));

            // 조회 대상이 되는 리스트 조회
            Page<DocumentListItemDto> result = documentService.findDocumentInfo(reqDto.toContentListItemDto(currentAccount), PageRequest.of(reqDto.getPage(), reqDto.getPageSize()));

            // 해당 테이블에 저장된
            List<Long> ids = result.stream()
                    .map(DocumentListItemDto::getDocNo)
                    .toList();

            // 원본 문서에 저장된 정보 조회
            List<ApprovalDocument> approvalDocuments = approvalDocumentRepository.findApprovalDocumentInfo(ids);

            return ContentMyDocResDto.fromEntity(result, approvalDocuments);
        }catch (EntityNotFoundException e) {
            log.warn("문서 목록 조회 실패 - 계정 없음 또는 데이터 누락: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("문서 목록 조회 중 시스템 오류 발생", e);
            throw new RuntimeException("문서 목록을 불러오는 중 오류가 발생했습니다.", e);
        }
    }


    /**
     * 문서 생성
     * */
    @Transactional
    public CreateDocumentDefaultInfoResDto setDocument() {
        try{
            ApprovalDocument approvalDocument = approvalDocumentService.createApprovalDocument();
            Document document = documentService.createDocument(approvalDocument);
            return CreateDocumentDefaultInfoResDto.fromDefaultInfo(document);
        }catch (Exception e) {
            log.error("문서 생성(초기화) 중 오류 발생", e);
            throw new RuntimeException("새 문서를 생성하는 데 실패했습니다.", e);
        }
    }

    /**
     * 문서 작성
     * */
    @Transactional
    public void setContent(ContentSetReqDto reqDto) {
        try{
            Long docNo = Long.valueOf(reqDto.getDocNo());
            ApprovalDocument approvalDocument = approvalDocumentRepository.findById(docNo)
                    .orElseThrow(() -> new EntityNotFoundException("해당 문서를 찾을 수 없습니다."));

            Document document = documentRepository.findByDocNo(docNo)
                    .orElseThrow(() -> new EntityNotFoundException("해당 콘텐츠 문서를 찾을 수 없습니다."));
            reqDto.updateDocument(document, approvalDocument);
        }catch (NumberFormatException e) {
            log.error("문서 저장 실패 - 잘못된 문서 번호 형식: {}", reqDto.getDocNo());
            throw new IllegalArgumentException("유효하지 않은 문서 번호입니다.", e);
        } catch (EntityNotFoundException e) {
            log.warn("문서 저장 실패 - 대상 없음: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("문서 저장 중 예기치 않은 오류 발생. DocNo={}", reqDto.getDocNo(), e);
            throw new RuntimeException("문서 저장 중 시스템 오류가 발생했습니다.", e);
        }

    }

    /**
     * 문서 상세조회
     * @param docNoStr
     * */
    public ContentGetResDto getDocumentInfo(String docNoStr) {
        try {
            Long docNo = Long.valueOf(docNoStr);

            ApprovalDocument approvalDocument = approvalDocumentRepository.findById(docNo)
                    .orElseThrow(() -> new EntityNotFoundException("해당 결재 문서를 찾을 수 없습니다."));

            Document document = documentRepository.findByDocNo(docNo)
                    .orElseThrow(() -> new EntityNotFoundException("해당 콘텐츠 문서를 찾을 수 없습니다."));

            return ContentGetResDto.fromEntity(document, approvalDocument);

        } catch (NumberFormatException e) {
            log.error("상세 조회 실패 - 잘못된 ID 형식: {}", docNoStr);
            throw new IllegalArgumentException("문서 번호가 올바르지 않습니다.");
        } catch (EntityNotFoundException e) {
            log.warn("상세 조회 실패 - 데이터 없음: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("상세 조회 중 시스템 오류 발생. ID={}", docNoStr, e);
            throw new RuntimeException("문서 정보를 불러오는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 문서 조회수 업데이트
     * @param reqDto
     * */
    @Transactional
    public void updateViewCount(UpdateViewCountReqDto reqDto) {
        try{
            Long id = Long.valueOf(reqDto.getDocNo());

            documentRepository.findByDocNo(id)
                    .orElseThrow(() -> new EntityNotFoundException("해당 문서를 찾을 수 없습니다."));

            documentRepository.addViewCnt(id, 1);

        }catch (NumberFormatException e) {
            log.error("조회수 증가 실패 - 잘못된 ID: {}", reqDto.getDocNo());
            throw new IllegalArgumentException("잘못된 문서 번호입니다.");
        } catch (EntityNotFoundException e) {
            log.warn("조회수 증가 실패 - 문서 없음: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("조회수 증가 업데이트 중 오류 발생", e);
            // 롤백을 위해 예외 전파
            throw new RuntimeException("조회수 업데이트 실패", e);
        }
    }
}
