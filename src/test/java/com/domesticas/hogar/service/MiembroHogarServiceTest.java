package com.domesticas.hogar.service;

import com.domesticas.exception.BadRequestException;
import com.domesticas.hogar.model.Hogar;
import com.domesticas.hogar.model.MiembroHogar;
import com.domesticas.hogar.model.Rol;
import com.domesticas.hogar.repository.MiembroHogarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MiembroHogarServiceTest {

    @Mock
    private MiembroHogarRepository miembroHogarRepository;

    @InjectMocks
    private MiembroHogarService miembroHogarService;

    private Hogar hogar;
    private Rol rolMadre, rolTutor, rolHijo;

    @BeforeEach
    void setUp() {
        hogar = Hogar.builder().id(1L).build();
        rolMadre = Rol.builder().nombre("Madre").build();
        rolTutor = Rol.builder().nombre("Tutor").build();
        rolHijo = Rol.builder().nombre("Hijo").build();
    }

    @Test
    @DisplayName("Eliminar miembro que NO es admin - No debe reasignar")
    void eliminarMiembro_NoEsAdmin_SoloElimina() {
        MiembroHogar miembro = MiembroHogar.builder()
                .id(1L).hogar(hogar).esAdministrador(false).rol(rolHijo).build();

        when(miembroHogarRepository.findById(1L)).thenReturn(Optional.of(miembro));

        miembroHogarService.eliminarMiembro(1L);

        verify(miembroHogarRepository, times(1)).delete(miembro);
        verify(miembroHogarRepository, never()).findByHogarId(anyLong());
    }

    @Test
    @DisplayName("Eliminar admin - Debe buscar nuevo admin por prioridad (Madre)")
    void eliminarMiembro_EsAdmin_ReasignaMadre() {
        MiembroHogar admin = MiembroHogar.builder()
                .id(1L).hogar(hogar).esAdministrador(true).rol(rolHijo).build();
        MiembroHogar posibleAdmin = MiembroHogar.builder()
                .id(2L).rol(rolMadre).esAdministrador(false).build();

        when(miembroHogarRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(miembroHogarRepository.findByHogarId(1L)).thenReturn(List.of(admin, posibleAdmin));

        miembroHogarService.eliminarMiembro(1L);

        assertTrue(posibleAdmin.getEsAdministrador());
        verify(miembroHogarRepository).save(posibleAdmin);
    }

    @Test
    @DisplayName("Prioridad: Si no hay Madre, debe elegir Tutor")
    void buscarPorPrioridad_EligeTutorSiNoHayMadre() {
        MiembroHogar admin = MiembroHogar.builder()
                .id(1L).hogar(hogar).esAdministrador(true).rol(rolHijo).build();
        MiembroHogar tutor = MiembroHogar.builder()
                .id(2L).rol(rolTutor).esAdministrador(false).build();

        when(miembroHogarRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(miembroHogarRepository.findByHogarId(1L)).thenReturn(List.of(admin, tutor));

        miembroHogarService.eliminarMiembro(1L);

        assertTrue(tutor.getEsAdministrador());
        verify(miembroHogarRepository).save(tutor);
    }

    @Test
    @DisplayName("Si el miembro no existe, lanza BadRequestException")
    void eliminarMiembro_NoEncontrado_LanzaExcepcion() {
        when(miembroHogarRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> miembroHogarService.eliminarMiembro(99L));
    }

    @Test
    @DisplayName("Si no quedan miembros al eliminar admin, no debe fallar")
    void reasignarAdministrador_ListaVacia_NoHaceNada() {
        MiembroHogar admin = MiembroHogar.builder()
                .id(1L).hogar(hogar).esAdministrador(true).rol(rolHijo).build();

        when(miembroHogarRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(miembroHogarRepository.findByHogarId(1L)).thenReturn(List.of());

        assertDoesNotThrow(() -> miembroHogarService.eliminarMiembro(1L));
        verify(miembroHogarRepository, never()).save(any());
    }
}