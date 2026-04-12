package com.section.admin.product.controller;

import com.section.admin.product.req.CategorySaveRequest;
import com.section.admin.product.service.AdminCategoryService;
import com.section.common.commerce.entity.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/categories")
public class AdminCategoryRestController {

    private final AdminCategoryService adminCategoryService;

    @GetMapping("/list")
    public ResponseEntity<List<Category>> getList(@RequestParam(value = "depth", defaultValue = "1") Integer depth) {
        return ResponseEntity.ok(adminCategoryService.getCategoryListByDepth(depth));
    }

    @GetMapping("/sub")
    public ResponseEntity<List<Category>> getSubList(@RequestParam("parentNo") Long parentNo) {
        return ResponseEntity.ok(adminCategoryService.getSubCategories(parentNo));
    }

    @PostMapping("/save")
    public ResponseEntity<Void> save(@RequestBody CategorySaveRequest req) {
        adminCategoryService.saveCategory(req);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam("no") Long categoryNo) {
        adminCategoryService.deleteCategory(categoryNo);
        return ResponseEntity.ok().build();
    }
}
