package com.miapp.modulocontrataciones.Services.Eventos;

import com.miapp.modulocontrataciones.Daos.EventoDAO;
import com.miapp.sistemasdistribuidos.dto.EventoDTO;
import com.miapp.sistemasdistribuidos.entity.Evento;
import com.miapp.sistemasdistribuidos.entity.Trabajador;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Service
@Transactional
public class EventoServiceDemo implements IEventoService {
    private static final Logger logger = LoggerFactory.getLogger(EventoServiceDemo.class);

    @Autowired
    private EventoDAO eventoDAO;

    private EventoDTO convertToDTO(Evento evento) {
        EventoDTO dto = new EventoDTO();
        dto.setFecha(evento.getFecha());
        dto.setTrabajadorId(evento.getTrabajadorId().getTrabajadorId());
        dto.setEventoId(evento.getEventoId());
        dto.setLugar(evento.getLugar());
        return dto;
    }

    private Evento convertToEntity(EventoDTO dto) {
        Evento evento = new Evento();
        evento.setFecha(dto.getFecha());
        evento.setLugar(dto.getLugar());
        evento.setEventoId(dto.getEventoId());

        Trabajador trabajador = new Trabajador();
        trabajador.setTrabajadorId(dto.getTrabajadorId());
        evento.setTrabajadorId(trabajador);
        return evento;
    }

    @Override
    public EventoDTO getById(Integer id) {
        try {
            Optional<Evento> evento = eventoDAO.findById(id);
            if (evento.isEmpty()) {
                logger.error("No se encontró el evento con el ID: {}", id);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado");
            }
            EventoDTO result = convertToDTO(evento.get());
            logger.info("Retornando evento con ID: {}", id);
            return result;
        } catch (ResponseStatusException e) {
            logger.error("Error al obtener el evento: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado al obtener el evento: {}", e);
            throw e;
        }
    }

    @Override
    public EventoDTO create(EventoDTO eventoDTO) {
        try {
            Evento evento = convertToEntity(eventoDTO);
            Evento savedEvento = eventoDAO.save(evento);
            EventoDTO result = convertToDTO(savedEvento);
            logger.info("Evento creado con éxito");
            return result;
        } catch (Exception e) {
            logger.error("Error durante la creación del evento: {}", e);
            throw e;
        }
    }

    @Override
    public EventoDTO update(EventoDTO eventoDTO) {
        try {
            Optional<Evento> existingEvento = eventoDAO.findById(eventoDTO.getEventoId());
            if (existingEvento.isPresent()) {
                Evento evento = convertToEntity(eventoDTO);
                eventoDAO.save(evento);
                EventoDTO result = convertToDTO(evento);
                logger.info("Evento actualizado con éxito");
                return result;
            } else {
                logger.warn("No se encontró el evento con el ID proporcionado");
                return null;
            }
        } catch (Exception e) {
            logger.error("Error al actualizar el evento: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public void delete(Integer id) {
        try {
            eventoDAO.deleteById(id);
            logger.info("Evento eliminado con éxito");
        } catch (Exception e) {
            logger.error("Error al eliminar el evento: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public Page<EventoDTO> getAll(Pageable pageable) {
        try {
            Page<Evento> eventos = eventoDAO.findAll(pageable);

            if (eventos.hasContent()) {
                List<EventoDTO> eventoDTOs = new ArrayList<>();
                eventos.forEach(evento -> {
                    EventoDTO eventoDTO = convertToDTO(evento);
                    eventoDTOs.add(eventoDTO);
                });
                logger.info("Lista de eventos paginada obtenida correctamente desde la base de datos.");
                return new PageImpl<>(eventoDTOs, pageable, eventos.getTotalElements());
            }

            logger.info("No se encontraron eventos en la base de datos.");
            return Page.empty(pageable);

        } catch (Exception e) {
            logger.error("Error al obtener los eventos paginados: {}", e);
            throw e;
        }
    }
}
