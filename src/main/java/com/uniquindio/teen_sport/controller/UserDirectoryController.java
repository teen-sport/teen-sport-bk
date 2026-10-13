package com.uniquindio.teen_sport.controller;

import com.uniquindio.teen_sport.dto.CoachListItem;
import com.uniquindio.teen_sport.dto.GuardianListItem;
import com.uniquindio.teen_sport.dto.PageResponse;
import com.uniquindio.teen_sport.entity.EntityStatus;
import com.uniquindio.teen_sport.service.UserDirectoryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Listados de entrenadores (U11) y acudientes (U12), exclusivos del rol {@code ADMIN}.
 */
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class UserDirectoryController {

    private final UserDirectoryService directoryService;

    /**
     * @param directoryService listados de apoyo
     */
    public UserDirectoryController(UserDirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    /**
     * U11: entrenadores, para asignarlos a grupos.
     *
     * @param status filtro por estado de la cuenta
     * @return 200 con los entrenadores
     */
    @GetMapping("/coaches")
    public List<CoachListItem> listCoaches(@RequestParam(required = false) EntityStatus status) {
        return directoryService.listCoaches(status);
    }

    /**
     * U12: acudientes paginados, para registrar deportistas en su nombre.
     *
     * @param page índice de página, desde 0
     * @param size tamaño de página, de 1 a 100
     * @param q    búsqueda libre sobre nombre, correo y documento
     * @return 200 con la página
     */
    @GetMapping("/guardians")
    public PageResponse<GuardianListItem> listGuardians(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                        @RequestParam(required = false) @Size(max = 150) String q) {
        return directoryService.listGuardians(q, page, size);
    }
}
