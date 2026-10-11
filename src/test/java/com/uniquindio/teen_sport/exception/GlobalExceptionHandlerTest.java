package com.uniquindio.teen_sport.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GlobalExceptionHandlerTest.FailingController.class)
@Import(GlobalExceptionHandlerTest.FailingController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void businessExceptionUsesItsStatusAndCode() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("NO_AVAILABLE_SPOTS"))
                .andExpect(jsonPath("$.message").value("No hay cupos disponibles."))
                .andExpect(jsonPath("$.details", hasSize(0)))
                .andExpect(jsonPath("$.path").value("/test/business"))
                .andExpect(jsonPath("$.timestamp", containsString("T")));
    }

    @Test
    void invalidBodyReturnsValidationErrorWithFieldDetailsInSpanish() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.INVALID_FIELDS_MESSAGE))
                .andExpect(jsonPath("$.details", hasSize(1)))
                .andExpect(jsonPath("$.details[0].field").value("name"))
                .andExpect(jsonPath("$.details[0].message").value("no debe estar vacío"));
    }

    @Test
    void malformedJsonReturnsValidationErrorWithoutParserDetails() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.UNREADABLE_BODY_MESSAGE))
                .andExpect(jsonPath("$.details", hasSize(0)));
    }

    @Test
    void invalidQueryParameterReturnsValidationErrorWithParameterName() throws Exception {
        mockMvc.perform(get("/test/page").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("size"))
                .andExpect(jsonPath("$.details[0].message", not(emptyString())));
    }

    @Test
    void missingQueryParameterReturnsValidationError() throws Exception {
        mockMvc.perform(get("/test/page"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("size"));
    }

    @Test
    void wrongPathVariableTypeReturnsValidationError() throws Exception {
        mockMvc.perform(get("/test/items/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("id"));
    }

    @Test
    void unknownRouteReturnsNotFound() throws Exception {
        mockMvc.perform(get("/test/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.RESOURCE_NOT_FOUND_MESSAGE));
    }

    @Test
    void unexpectedFailureReturnsInternalErrorWithoutInternalDetails() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.UNEXPECTED_ERROR_MESSAGE))
                .andExpect(jsonPath("$.details", hasSize(0)))
                .andExpect(jsonPath("$.path").value("/test/unexpected"))
                .andExpect(content().string(not(containsString("SELECT"))))
                .andExpect(content().string(not(containsString("secreto"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void unsupportedMethodKeepsItsStatusInsteadOfInternalError() throws Exception {
        mockMvc.perform(post("/test/business"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unsupportedMediaTypeKeepsItsStatusInsteadOfInternalError() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name"))
                .andExpect(status().isUnsupportedMediaType());
    }

    record NameRequest(@NotBlank String name) {
    }

    @RestController
    static class FailingController {

        @GetMapping("/test/business")
        void business() {
            throw new BusinessException(ErrorCode.NO_AVAILABLE_SPOTS, HttpStatus.UNPROCESSABLE_CONTENT,
                    "No hay cupos disponibles.");
        }

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("SELECT * FROM users WHERE password = 'secreto'");
        }

        @PostMapping("/test/body")
        void body(@Valid @RequestBody NameRequest request) {
        }

        @GetMapping("/test/page")
        void page(@RequestParam @Max(100) int size) {
        }

        @GetMapping("/test/items/{id}")
        void item(@PathVariable long id) {
        }
    }
}
