package com.domesticas.tarea.repository;
import java.util.List;
import com.domesticas.tarea.model.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TareaRepository extends JpaRepository<Tarea, Long> {


    List<Tarea> findByHogarId(Long hogarId);

    List<Tarea> findByHogarIdAndUsuarioId(Long hogarId, Long usuarioId);

    List<Tarea> findByHogarIdAndEstado(Long hogarId, String estado);

    List<Tarea> findByHogarIdAndUsuarioIdAndEstado(Long hogarId, Long usuarioId, String estado);


}