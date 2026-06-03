package com.section.admin.task.support;

import com.section.common.base.entity.type.AdminOperationTaskPriority;
import com.section.common.base.entity.type.AdminOperationTaskStatus;
import com.section.common.system.dto.AdminOperationTaskListResDto;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

public final class AdminOperationTaskExportCsvWriter {
    private static final String HEADER = "작업번호,제목,설명,상태,우선순위,담당자,마감일,마감상태,고정여부,메모수,최근메모,최근메모작성자,최근메모일시,등록일시";
    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private AdminOperationTaskExportCsvWriter() {
    }

    public static byte[] write(
            AdminOperationTaskExportSummary summary,
            List<AdminOperationTaskListResDto> tasks,
            LocalDate today
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append(csv("내보낸시각")).append(',').append(csv(summary.exportedAt())).append("\r\n");
        builder.append(csv("정렬")).append(',').append(csv(summary.sortLabel())).append("\r\n");
        builder.append(csv("조회조건")).append(',').append(csv(summary.filterSummary())).append("\r\n");
        builder.append("\r\n");
        builder.append(HEADER).append("\r\n");

        for (AdminOperationTaskListResDto task : tasks) {
            builder.append(csv(String.valueOf(task.getTaskNo()))).append(',')
                    .append(csv(task.getTitle())).append(',')
                    .append(csv(task.getDescription())).append(',')
                    .append(csv(AdminOperationTaskStatus.fromCode(task.getStatus()).getLabel())).append(',')
                    .append(csv(AdminOperationTaskPriority.fromCode(task.getPriority()).getLabel())).append(',')
                    .append(csv(task.getAssigneeAdminName() == null || task.getAssigneeAdminName().isBlank() ? "미지정" : task.getAssigneeAdminName())).append(',')
                    .append(csv(task.getDueDate() == null ? "-" : task.getDueDate().toString())).append(',')
                    .append(csv(resolveDueState(task, today))).append(',')
                    .append(csv("Y".equalsIgnoreCase(task.getIsPinned()) ? "고정" : "일반")).append(',')
                    .append(csv(String.valueOf(task.getCommentCount() == null ? 0L : task.getCommentCount()))).append(',')
                    .append(csv(task.getLatestCommentContent())).append(',')
                    .append(csv(task.getLatestCommentAdminName())).append(',')
                    .append(csv(task.getLatestCommentDtm() == null ? "-" : task.getLatestCommentDtm().toString().replace('T', ' '))).append(',')
                    .append(csv(task.getCrtDtm() == null ? "-" : task.getCrtDtm().toString().replace('T', ' ')))
                    .append("\r\n");
        }

        byte[] body = builder.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(UTF8_BOM.length + body.length);
        outputStream.writeBytes(UTF8_BOM);
        outputStream.writeBytes(body);
        return outputStream.toByteArray();
    }

    private static String resolveDueState(AdminOperationTaskListResDto task, LocalDate today) {
        if (task.getDueDate() == null) {
            return "기한 없음";
        }
        if ("DONE".equalsIgnoreCase(task.getStatus())) {
            return "완료";
        }
        if (task.getDueDate().isBefore(today)) {
            return "기한 초과";
        }
        if (task.getDueDate().isEqual(today)) {
            return "오늘 마감";
        }
        return "진행중";
    }

    private static String csv(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
