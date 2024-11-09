package com.miapp.modulocontrataciones.Services.Participantes;

import com.miapp.sistemasdistribuidos.dto.EventoDTO;
import com.miapp.sistemasdistribuidos.dto.ParticipantesDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
public interface IParticipantesService {
    Optional<ParticipantesDTO> getByID(Integer id);
    EventoDTO create(ParticipantesDTO participantesDTO);
    EventoDTO update(Integer id, ParticipantesDTO participantesDTO);
    void delete(Integer id);

    Page<ParticipantesDTO> getByActividad(Integer actividadId, Pageable pageable);
}
