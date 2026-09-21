package com.example.fandoom_backend.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Spring'in kendi client-hatası exception'ları (405/415/bozuk JSON) catch-all'a düşüp 500 + ERROR stacktrace vermemeli.
class GlobalExceptionHandlerFrameworkErrorsTest {

    record Body(String name) {
    }

    @RestController
    static class Probe {
        @GetMapping("/probe")
        String get() {
            return "ok";
        }

        @PostMapping(value = "/probe-json", consumes = MediaType.APPLICATION_JSON_VALUE)
        String post(@RequestBody Body body) {
            return "ok";
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new Probe()).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void unsupportedMethod_is405_withAllowHeader() throws Exception {
        mockMvc.perform(delete("/probe"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void malformedJson_is400() throws Exception {
        mockMvc.perform(post("/probe-json").contentType(MediaType.APPLICATION_JSON).content("{bozuk"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unsupportedMediaType_is415() throws Exception {
        mockMvc.perform(post("/probe-json").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }
}
