package com.section.common.system.repository;

import com.section.common.system.dto.AdminOperationTaskCommentResDto;

import java.util.List;

public interface CustomAdminOperationTaskCommentRepository {

    List<AdminOperationTaskCommentResDto> getTaskComments(Long taskNo, int limit);
}
