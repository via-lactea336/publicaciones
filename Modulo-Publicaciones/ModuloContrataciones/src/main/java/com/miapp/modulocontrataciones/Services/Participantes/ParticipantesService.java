package com.miapp.modulocontrataciones.Services.Participantes;

import com.miapp.modulocontrataciones.Daos.ActividadesDAO;
import com.miapp.sistemasdistribuidos.dto.EventoDTO;
import com.miapp.sistemasdistribuidos.dto.ParticipantesDTO;
import com.miapp.modulocontrataciones.Daos.ParticipantesDAO;
import com.miapp.sistemasdistribuidos.entity.Actividades;
import com.miapp.sistemasdistribuidos.entity.Cliente;
import com.miapp.sistemasdistribuidos.entity.Participantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;

@Service
@Transactional
public class ParticipantesService implements IParticipantesService {
    private static final Logger logger = LoggerFactory.getLogger(ParticipantesService.class);

    @Autowired
    private ParticipantesDAO participantesDAO;

    @Autowired
    private ActividadesDAO actividadesDAO;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "participanteById::";

    private ParticipantesDTO convertToDTO(Participantes participante) {
        ParticipantesDTO dto = new ParticipantesDTO();
        dto.setParticipanteId(participante.getParticipanteId());
        dto.setClienteId(participante.getClienteId().getClienteId());
        dto.setActividadId(participante.getActividadId().getActividadId());
        return dto;
    }

    private Participantes convertToEntity(ParticipantesDTO dto) {
        Participantes participante = new Participantes();
        participante.setParticipanteId(dto.getParticipanteId());

        Cliente cliente = new Cliente();
        cliente.setClienteId(dto.getClienteId());
        participante.setClienteId(cliente);

        Actividades actividad = new Actividades();
        actividad.setActividadId(dto.getActividadId());
        participante.setActividadId(actividad);
        return participante;
    }

    @Override
    @Cacheable(value = "participanteById", key = "#id", unless = "#result == null")
    public Optional<ParticipantesDTO> getByID(Integer id) {
        try {
            Optional<Participantes> participante = participantesDAO.findById(id);
            if (participante.isEmpty()) {
                logger.error("No se encontró el participante con el ID: {}" , id);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Participante no encontrado");
            }
            Optional<ParticipantesDTO> result = participante.map(this::convertToDTO);
            logger.info("Retornando participante con ID: {}" , id);
            return result;
        } catch (ResponseStatusException e) {
            logger.error("Error al obtener el participante: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado al obtener el participante: {}", e);
            throw e;
        }
    }

    @Override
    @CachePut(value = "participanteById", key = "#result.id")
    public EventoDTO create(ParticipantesDTO participantesDTO) {
        try {
            Participantes participante = convertToEntity(participantesDTO);
            Optional<Participantes> savedParticipante = Optional.of(participantesDAO.save(participante));
            logger.info("Participante creado con éxito");
            eliminarTodasLasClavesDePagina();
            Actividades actividad = actividadesDAO.getById(participantesDTO.getActividadId());
            actividad.setCantidad(actividad.getCantidad() + 1);
            actividadesDAO.save(actividad);
            return new EventoDTO();
        } catch (Exception e) {
            logger.error("Error durante la creación del participante: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @CachePut(value = "participanteById", key = "#id")
    public EventoDTO update(Integer id, ParticipantesDTO participantesDTO) {
        try {
            Optional<Participantes> existingParticipante = participantesDAO.findById(id);
            if (existingParticipante.isPresent()) {
                Participantes participante = convertToEntity(participantesDTO);
                participantesDAO.save(participante);
                ParticipantesDTO result = convertToDTO(participante);
                logger.info("Participante actualizado con éxito");
                return new EventoDTO();
            } else {
                logger.warn("No se encontró el participante con el ID proporcionado");
                return null;
            }
        } catch (Exception e) {
            logger.error("Error al actualizar el participante: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @CacheEvict(value = "participanteById", key = "#id")
    public void delete(Integer id) {
        try {
            Participantes participante = participantesDAO.getById(id);
            Actividades actividad = actividadesDAO.getById(participante.getActividadId().getActividadId());
            actividad.setCantidad(actividad.getCantidad() + 1);
            actividadesDAO.save(actividad);
            participantesDAO.deleteById(id);
            logger.info("Participante eliminado con éxito");
            eliminarTodasLasClavesDePagina();
        } catch (Exception e) {
            logger.error("Error al eliminar el participante: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public Page<ParticipantesDTO> getByActividad(Integer actividadId, Pageable pageable) {
        try {
            String pageIdsCacheKey = CACHE_KEY_PREFIX + "PAGE_IDS_" + pageable.getPageNumber();
            List<Integer> participanteIds = (List<Integer>) redisTemplate.opsForValue().get(pageIdsCacheKey);
            List<ParticipantesDTO> participantesCache = new ArrayList<>();

            if (participanteIds != null && !participanteIds.isEmpty()) {
                for (Integer participanteId : participanteIds) {
                    ParticipantesDTO participanteDTO = (ParticipantesDTO) redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + participanteId);
                    if (participanteDTO != null) {
                        participantesCache.add(participanteDTO);
                    }
                }
                if (!participantesCache.isEmpty()) {
                    logger.info("Participantes obtenidos desde el cache para la página: " + pageable.getPageNumber());
                    return new PageImpl<>(participantesCache, pageable, participantesCache.size());
                }
            }
            Actividades actividad = new Actividades();
            actividad.setActividadId(actividadId);
            Page<Participantes> participantes = participantesDAO.getAllByActividadId(actividad, pageable);

            if (participantes.hasContent()) {
                List<Integer> nuevosParticipanteIds = new ArrayList<>();
                List<ParticipantesDTO> participanteDTOs = new ArrayList<>();

                participantes.forEach(participante -> {
                    ParticipantesDTO participanteDTO = convertToDTO(participante);
                    participanteDTOs.add(participanteDTO);

                    redisTemplate.opsForValue().set(CACHE_KEY_PREFIX + participante.getParticipanteId(), participanteDTO, 10, TimeUnit.MINUTES);
                    nuevosParticipanteIds.add(participante.getParticipanteId());
                });

                redisTemplate.opsForValue().set(pageIdsCacheKey, nuevosParticipanteIds, 10, TimeUnit.MINUTES);

                logger.info("Lista de participantes paginada obtenida correctamente desde la base de datos y almacenada en caché.");
                return new PageImpl<>(participanteDTOs, pageable, participantes.getTotalElements());
            }

            logger.info("No se encontraron participantes en la base de datos.");
            return Page.empty(pageable);

        } catch (Exception e) {
            logger.error("Error al obtener los participantes paginados: ", e);
            throw e;
        }
    }

    private void eliminarTodasLasClavesDePagina() {
        String patron = CACHE_KEY_PREFIX + "PAGE_IDS_*";
        Set<String> claves = redisTemplate.keys(patron);

        if (claves != null && !claves.isEmpty()) {
            redisTemplate.delete(claves);
            logger.info(claves.size() + " claves eliminadas que coinciden con el patrón: " + patron);
        } else {
            logger.info("No se encontraron claves que coincidan con el patrón: " + patron);
        }
    }
}
