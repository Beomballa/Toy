package com.section.front.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class FrontViewControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new FrontViewController()).build();

    @Test
    @DisplayName("프론트 루트 화면은 메인 뷰를 반환한다")
    void indexReturnsMainView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/index"));
    }

    @Test
    @DisplayName("프론트 별칭 경로도 메인 뷰를 반환한다")
    void frontAliasReturnsMainView() throws Exception {
        mockMvc.perform(get("/front"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/index"));
    }
}
