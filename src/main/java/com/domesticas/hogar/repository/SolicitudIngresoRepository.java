package com.domesticas.hogar.repository;
import com.domesticas.hogar.model.Hogar;
import com.domesticas.hogar.model.SolicitudIngreso;
import com.domesticas.usuario.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SolicitudIngresoRepository extends JpaRepository<SolicitudIngreso, Long> {

    boolean existsByUsuarioAndHogarAndEstado(Usuario usuario, Hogar hogar, String estado);
   
    List<SolicitudIngreso> findByUsuarioId(Long usuarioId);
}

