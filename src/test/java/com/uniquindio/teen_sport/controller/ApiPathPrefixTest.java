package com.uniquindio.teen_sport.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.uniquindio.teen_sport.config.ClockConfig;
import com.uniquindio.teen_sport.exception.ApiErrorFactory;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prefijo {@code /api/v1} de los controladores.
 *
 * <p>Aísla la capa MVC: los filtros de seguridad se desactivan porque aquí solo se prueba
 * el enrutamiento; la seguridad tiene sus propias pruebas de integración.</p>
 */
@WebMvcTest(ApiPathPrefixTest.ProbeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ApiPathPrefixTest.ProbeController.class, ApiErrorFactory.class, ClockConfig.class})
class ApiPathPrefixTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void controllersInControllerPackageAreServedUnderApiV1() throws Exception {
        mockMvc.perform(get("/api/v1/probe"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void controllersAreNotServedWithoutPrefix() throws Exception {
        mockMvc.perform(get("/probe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/probe"));
    }

    @RestController
    static class ProbeController {

        @GetMapping("/probe")
        String probe() {
            return "ok";
        }
    }
}
