package com.section.front.content.exception;

public class FrontContentNotFoundException extends RuntimeException {

    public FrontContentNotFoundException() {
        super("공개 콘텐츠를 찾을 수 없습니다.");
    }
}
