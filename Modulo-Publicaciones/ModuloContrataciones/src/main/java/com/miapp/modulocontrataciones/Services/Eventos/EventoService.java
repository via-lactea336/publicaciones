package com.miapp.modulocontrataciones.Services.Eventos;

import com.miapp.modulocontrataciones.Daos.EventoDAO;
import com.miapp.sistemasdistribuidos.dto.EventoDTO;
import com.miapp.sistemasdistribuidos.entity.Evento;
import com.miapp.sistemasdistribuidos.entity.Trabajador;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class EventoService implements IEventoService {
    private static final Logger logger = LoggerFactory.getLogger(EventoService.class);

    @Autowired
    private EventoDAO eventoDAO;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "eventoById::";

    private EventoDTO convertToDTO(Evento evento) {
        EventoDTO dto = new EventoDTO();
        dto.setFecha(evento.getFecha());
        dto.setTrabajadorId(evento.getTrabajadorId().getTrabajadorId());
        dto.setEventoId(evento.getEventoId());
        dto.setLugar(evento.getLugar());
        dto.setNombreEvento(evento.getNombreEvento());
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
        evento.setNombreEvento(dto.getNombreEvento());
        return evento;
    }

    @Override
    @Cacheable(value = "eventoById", key = "#id", unless = "#result == null")
    public EventoDTO getById(Integer id) {
        try {
            Optional<Evento> evento = eventoDAO.findById(id);
            if (evento.isEmpty()) {
                logger.error("No se encontró el evento con el ID: {}" + id);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado");
            }
            EventoDTO result = convertToDTO(evento.get());
            logger.info("Retornando evento con ID: {}" + id);
            return result;
        } catch (ResponseStatusException e) {
            logger.error("Error al obtener el evento: {}" + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado al obtener el evento: {}", e);
            throw e;
        }
    }

    @Override
    @CachePut(value = "eventoById", key = "#result.eventoId")
    public EventoDTO create(EventoDTO eventoDTO) {
        try {
            Evento evento = convertToEntity(eventoDTO);
            Evento savedEvento = eventoDAO.save(evento);
            EventoDTO result = convertToDTO(savedEvento);
            logger.info("Evento creado con éxito");
            eliminarTodasLasClavesDePagina();
            return result;
        } catch (Exception e) {
            logger.error("Error durante la creación del evento: {}", e);
            throw e;
        }
    }
    @Override
    @CachePut(value = "eventoById", key = "#eventoDTO.eventoId")
    public EventoDTO update(EventoDTO eventoDTO) {
        try {
            Optional<Evento> existingEvento = eventoDAO.findById(eventoDTO.getEventoId());
            if (existingEvento.isPresent()) {
                Evento evento = convertToEntity(eventoDTO);

                // Validar el campo nombreEvento
                if (evento.getNombreEvento() == null || evento.getNombreEvento().isEmpty()) {
                    throw new IllegalArgumentException("El nombre del evento no puede ser nulo o vacío.");
                }

                eventoDAO.save(evento);
                EventoDTO result = convertToDTO(evento);
                logger.info("Evento actualizado con éxito: {}", result.toString());
                return result;
            } else {
                logger.warn("No se encontró el evento con el ID proporcionado: {}", eventoDTO.getEventoId());
                return null;
            }
        } catch (Exception e) {
            logger.error("Error al actualizar el evento: {}", e.getMessage());
            throw e;
        }
    }


    @Override
    @CacheEvict(value = "eventoById", key = "#id")
    public void delete(Integer id) {
        try {
            eventoDAO.deleteById(id);
            logger.info("Evento eliminado con éxito");
            eliminarTodasLasClavesDePagina();
        } catch (Exception e) {
            logger.error("Error al eliminar el evento: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public Page<EventoDTO> getAll(Pageable pageable) {
        try {
            String pageIdsCacheKey = CACHE_KEY_PREFIX + "PAGE_IDS_" + pageable.getPageNumber();
            List<Integer> eventoIds = (List<Integer>) redisTemplate.opsForValue().get(pageIdsCacheKey);
            List<EventoDTO> eventosCache = new ArrayList<>();

            if (eventoIds != null && !eventoIds.isEmpty()) {
                for (Integer eventoId : eventoIds) {
                    EventoDTO eventoDTO = (EventoDTO) redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + eventoId);
                    if (eventoDTO != null) {
                        eventosCache.add(eventoDTO);
                    }
                }
                if (!eventosCache.isEmpty()) {
                    logger.info("Eventos obtenidos desde el cache para la página: " + pageable.getPageNumber());
                    return new PageImpl<>(eventosCache, pageable, eventosCache.size());
                }
            }

            Page<Evento> eventos = eventoDAO.findAll(pageable);

            if (eventos.hasContent()) {
                List<Integer> nuevosEventoIds = new ArrayList<>();
                List<EventoDTO> eventoDTOs = new ArrayList<>();

                eventos.forEach(evento -> {
                    EventoDTO eventoDTO = convertToDTO(evento);
                    eventoDTOs.add(eventoDTO);

                    redisTemplate.opsForValue().set(CACHE_KEY_PREFIX + evento.getEventoId(), eventoDTO, 10, TimeUnit.MINUTES);
                    nuevosEventoIds.add(evento.getEventoId());
                });

                redisTemplate.opsForValue().set(pageIdsCacheKey, nuevosEventoIds, 10, TimeUnit.MINUTES);

                logger.info("Lista de eventos paginada obtenida correctamente desde la base de datos y almacenada en caché.");
                return new PageImpl<>(eventoDTOs, pageable, eventos.getTotalElements());
            }

            logger.info("No se encontraron eventos en la base de datos.");
            return Page.empty(pageable);

        } catch (Exception e) {
            logger.error("Error al obtener los eventos paginados: {}", e);
            throw e;
        }
    }

    private void eliminarTodasLasClavesDePagina() {
        String patron = CACHE_KEY_PREFIX + "PAGE_IDS_*";
        Set<String> claves = redisTemplate.keys(patron);

        if (claves != null && !claves.isEmpty()) {
            redisTemplate.delete(claves);
            logger.info(claves.size() + " claves eliminadas que coinciden con el patrón: {}" + patron);
        } else {
            logger.info("No se encontraron claves que coincidan con el patrón: {}" + patron);
        }
    }
}
