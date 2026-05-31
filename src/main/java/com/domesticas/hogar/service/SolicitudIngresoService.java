package com.domesticas.hogar.service;

import com.domesticas.exception.BadRequestException;
import com.domesticas.hogar.dto.response.MisSolicitudesResponse;
import com.domesticas.hogar.model.Hogar;
import com.domesticas.hogar.model.MiembroHogar;
import com.domesticas.hogar.model.Rol;
import com.domesticas.hogar.model.SolicitudIngreso;
import com.domesticas.hogar.repository.HogarRepository;
import com.domesticas.hogar.repository.MiembroHogarRepository;
import com.domesticas.hogar.repository.RolRepository;
import com.domesticas.hogar.repository.SolicitudIngresoRepository;
import com.domesticas.usuario.model.Usuario;
import com.domesticas.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SolicitudIngresoService {

    private final UsuarioRepository usuarioRepository;
    private final HogarRepository hogarRepository;
    private final MiembroHogarRepository miembroHogarRepository;
    private final SolicitudIngresoRepository solicitudIngresoRepository;
    private final RolRepository rolRepository;

    public void solicitarIngreso(String email, String codigoAcceso) {

        if (codigoAcceso == null || codigoAcceso.isBlank()) {
            throw new BadRequestException("El código de acceso es obligatorio");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        Hogar hogar = hogarRepository.findByCodigoAcceso(codigoAcceso)
                .orElseThrow(() -> new BadRequestException("Código inválido"));

        boolean yaEsMiembro = miembroHogarRepository
                .findByHogarIdAndUsuarioEmail(hogar.getId(), email)
                .isPresent();

        if (yaEsMiembro) {
            throw new BadRequestException("Ya perteneces a este grupo");
        }

        boolean existeSolicitud = solicitudIngresoRepository
                .existsByUsuarioAndHogarAndEstado(usuario, hogar, "PENDIENTE");

        if (existeSolicitud) {
            throw new BadRequestException("Ya tienes una solicitud pendiente");
        }

        SolicitudIngreso solicitud = SolicitudIngreso.builder()
                .usuario(usuario)
                .hogar(hogar)
                .estado("PENDIENTE")
                .build();

        solicitudIngresoRepository.save(solicitud);
    }
        public void responderSolicitud(Long solicitudId, String emailAdmin, String accion, String rolNombre) {

        SolicitudIngreso solicitud = solicitudIngresoRepository.findById(solicitudId)
                .orElseThrow(() -> new BadRequestException("Solicitud no encontrada"));

        Hogar hogar = solicitud.getHogar();

        MiembroHogar admin = miembroHogarRepository
                .findByHogarIdAndUsuarioEmail(hogar.getId(), emailAdmin)
                .orElseThrow(() -> new BadRequestException("No perteneces a este grupo"));

        if (!Boolean.TRUE.equals(admin.getEsAdministrador())) {
            throw new BadRequestException("Solo el administrador puede gestionar solicitudes");
        }

        if (!"PENDIENTE".equalsIgnoreCase(solicitud.getEstado())) {
            throw new BadRequestException("La solicitud ya fue gestionada");
        }

        if ("ACEPTAR".equalsIgnoreCase(accion)) {

            if (rolNombre == null || rolNombre.isBlank()) {
                throw new BadRequestException("Debes indicar el rol del usuario");
            }

            Rol rol = rolRepository.findByNombre(rolNombre)
                    .orElseThrow(() -> new BadRequestException("Rol no encontrado"));

            MiembroHogar nuevoMiembro = MiembroHogar.builder()
                    .usuario(solicitud.getUsuario())
                    .hogar(hogar)
                    .rol(rol)
                    .esAdministrador(false)
                    .build();

            miembroHogarRepository.save(nuevoMiembro);

            solicitud.setEstado("ACEPTADA");

        } else if ("RECHAZAR".equalsIgnoreCase(accion)) {

            solicitud.setEstado("RECHAZADA");

        } else {
            throw new BadRequestException("Acción inválida");
        }

        solicitudIngresoRepository.save(solicitud);
    }

    public List<MisSolicitudesResponse> obtenerMisSolicitudes(Long usuarioId) {

    List<SolicitudIngreso> solicitudes =
            solicitudIngresoRepository.findByUsuarioId(usuarioId);

    return solicitudes.stream()
            .map(s -> MisSolicitudesResponse.builder()
                    .id(s.getId())
                    .hogar(s.getHogar().getNombre())
                    .estado(s.getEstado())
                    .build())
            .toList();
}
}