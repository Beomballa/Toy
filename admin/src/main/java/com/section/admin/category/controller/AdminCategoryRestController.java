package com.section.admin.category.controller;

import com.section.admin.base.res.BaseSimpleResDto;
import com.section.admin.category.req.CategoryListRequest;
import com.section.admin.category.req.CategorySaveRequest;
import com.section.admin.category.req.CategoryStatusUpdateRequest;
import com.section.admin.category.res.CategoryResponse;
import com.section.admin.category.service.AdminCategoryService;
import com.section.admin.settings.service.AdminOperationPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/categories")
public class AdminCategoryRestController {

    private final AdminCategoryService adminCategoryService;
    private final AdminOperationPolicyService adminOperationPolicyService;

    @GetMapping("/list")
    public ResponseEntity<List<CategoryResponse>> getList(@ModelAttribute CategoryListRequest req) {
        return ResponseEntity.ok(adminCategoryService.getCategoryListByDepth(req));
    }

    @GetMapping("/sub")
    public ResponseEntity<List<CategoryResponse>> getSubList(@RequestParam("parentNo") Long parentNo) {
        return ResponseEntity.ok(adminCategoryService.getSubCategories(parentNo));
    }

    @PostMapping("/save")
    public ResponseEntity<Void> save(@Valid @RequestBody CategorySaveRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminCategoryService.saveCategory(req);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/active/{no}")
    public ResponseEntity<BaseSimpleResDto> updateActive(@PathVariable("no") Long categoryNo, @Valid @RequestBody CategoryStatusUpdateRequest req) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminCategoryService.updateActive(categoryNo, req.isActive());
        return ResponseEntity.ok(new BaseSimpleResDto());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam("no") Long categoryNo) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        adminCategoryService.deleteCategory(categoryNo);
        return ResponseEntity.ok().build();
    }
}
