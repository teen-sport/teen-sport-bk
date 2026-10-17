package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.dto.EditableProfile;
import com.uniquindio.teen_sport.entity.User;
import com.uniquindio.teen_sport.validation.TextNormalizer;
import org.springframework.stereotype.Component;

/**
 * Aplica los datos editables de un perfil (U4 y U9) con una sola regla:
 *
 * <ul>
 *   <li>Siempre se actualizan nombres, apellidos y teléfono.</li>
 *   <li>Solo se aplican los campos del rol de la cuenta: {@code homeAddress} para acudientes y
 *       {@code specialty}/{@code yearsOfExperience} para entrenadores. Los de otro rol se ignoran.</li>
 *   <li>Es un reemplazo completo: un campo opcional del rol que no se envía queda vacío.</li>
 * </ul>
 *
 * <p>Nunca modifica correo, documento, rol ni estado.</p>
 */
@Component
public class ProfileEditor {

    /**
     * @param user     cuenta a modificar
     * @param profiles perfil específico de la cuenta
     * @param data     nuevos datos
     */
    public void apply(User user, RoleProfileResolver.RoleProfiles profiles, EditableProfile data) {
        user.setFirstName(data.firstNames().trim());
        user.setLastName(data.lastNames().trim());
        user.setPhone(data.phone().trim());
        if (profiles.guardian() != null) {
            profiles.guardian().setAddress(TextNormalizer.trimToNull(data.homeAddress()));
        }
        if (profiles.coach() != null) {
            profiles.coach().setSpecialty(TextNormalizer.trimToNull(data.specialty()));
            profiles.coach().setYearsExperience(data.yearsOfExperience());
        }
    }
}
