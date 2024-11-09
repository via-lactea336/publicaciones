package com.miapp.modulocontrataciones.Services.Actividades;

import com.miapp.modulocontrataciones.Daos.ActividadesDAO;
import com.miapp.sistemasdistribuidos.dto.ActividadesDTO;
import com.miapp.sistemasdistribuidos.entity.Actividades;
import com.miapp.sistemasdistribuidos.entity.Evento;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class ActividadesService implements IActividadesService {

    private static final Logger logger = LoggerFactory.getLogger(ActividadesService.class);
    private static final String CACHE_KEY_PREFIX = "actividadById::";

    @Autowired
    private ActividadesDAO actividadesDAO;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private ActividadesDTO convertToDTO(Actividades actividad) {
        ActividadesDTO actividadDTO = new ActividadesDTO();
        actividadDTO.setActividadId(actividad.getActividadId());
        actividadDTO.setNombre(actividad.getNombre());
        actividadDTO.setDescripcion(actividad.getDescripcion());
        actividadDTO.setEventoId(actividad.getEventoId().getEventoId());
        return actividadDTO;
    }

    private Actividades convertToEntity(ActividadesDTO actividadDTO) {
        Actividades actividad = new Actividades();
        actividad.setActividadId(actividadDTO.getActividadId());
        actividad.setNombre(actividadDTO.getNombre());
        actividad.setDescripcion(actividadDTO.getDescripcion());
        actividad.setCantidad(0);

        Evento evento = new Evento();
        evento.setEventoId(actividadDTO.getEventoId());
        actividad.setEventoId(evento);
        return actividad;
    }

    @Override
    public Page<ActividadesDTO> getAllAByEventoID(Pageable pageable, Integer eventoID) {
        try {
            String pageIdsCacheKey = CACHE_KEY_PREFIX + "EVENTO_" + eventoID + "_PAGE_IDS_" + pageable.getPageNumber();
            List<Integer> actividadIds = (List<Integer>) redisTemplate.opsForValue().get(pageIdsCacheKey);
            List<ActividadesDTO> actividadesCache = new ArrayList<>();

            if (actividadIds != null && !actividadIds.isEmpty()) {
                for (Integer actividadId : actividadIds) {
                    ActividadesDTO actividadDTO = (ActividadesDTO) redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + actividadId);
                    if (actividadDTO != null) {
                        actividadesCache.add(actividadDTO);
                    } else {
                        redisTemplate.delete(CACHE_KEY_PREFIX + actividadId); // Limpia el cache de claves nulas
                    }
                }
                if (!actividadesCache.isEmpty()) {
                    logger.info("Actividades obtenidas desde el cache para el evento: {}", eventoID);
                    return new PageImpl<>(actividadesCache, pageable, actividadesCache.size());
                }
            }
            Evento evento = new Evento();
            evento.setEventoId(eventoID);
            Page<Actividades> actividades = actividadesDAO.findAllByEventoId(pageable, evento);

            if (actividades.hasContent()) {
                List<Integer> nuevosActividadIds = new ArrayList<>();
                List<ActividadesDTO> actividadesDTOs = new ArrayList<>();

                actividades.forEach(actividad -> {
                    ActividadesDTO actividadDTO = convertToDTO(actividad);
                    actividadesDTOs.add(actividadDTO);
                    redisTemplate.opsForValue().set(CACHE_KEY_PREFIX + actividad.getActividadId(), actividadDTO, 10, TimeUnit.MINUTES);
                    nuevosActividadIds.add(actividad.getActividadId());
                });

                redisTemplate.opsForValue().set(pageIdsCacheKey, nuevosActividadIds, 10, TimeUnit.MINUTES);

                logger.info("Lista de actividades por evento obtenida correctamente desde la base de datos y almacenada en caché.");
                return new PageImpl<>(actividadesDTOs, pageable, actividades.getTotalElements());
            }

            logger.info("No se encontraron actividades para el evento: {}", eventoID);
            return Page.empty(pageable);

        } catch (Exception e) {
            logger.error("Error al obtener las actividades paginadas por evento: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Cacheable(value = "actividadById", key = "#id", unless = "#result == null")
    public ActividadesDTO getById(Integer id) {
        return actividadesDAO.findById(id).map(this::convertToDTO).orElse(null);
    }

    @Override
    @CachePut(value = "actividadById", key = "#result.actividadId")
    public ActividadesDTO create(ActividadesDTO actividadesDTO) {
        if (actividadesDTO == null) {
            logger.error("El objeto actividadesDTO es nulo.");
            return null;
        }
        try {
            // Crear la entidad a partir del DTO
            Actividades actividad = convertToEntity(actividadesDTO);
            actividad.setCantidad(0);

            // Guardar la actividad y obtener el objeto guardado con el ID generado
            Actividades savedActividad = actividadesDAO.save(actividad);

            // Convertir la entidad guardada de vuelta a DTO
            ActividadesDTO result = convertToDTO(savedActividad);

            // Asegurarse de que el ID esté presente antes de usarlo en la caché
            if (result.getActividadId() == null) {
                logger.error("La actividad guardada no tiene un ID válido.");
                return null;
            }

            logger.info("Actividad creada con éxito con ID: {}", result.getActividadId());

            // Realizar la operación de caché solo cuando el ID sea válido
            eliminarTodasLasClavesDePagina();
            return result;
        } catch (Exception e) {
            logger.error("Error durante la creación de la actividad: {}", e.getMessage(), e);
            throw e;
        }
    }


    @Override
    @CachePut(value = "actividadById", key = "#actividadesDTO.actividadId")
    public ActividadesDTO update(ActividadesDTO actividadesDTO) {
        return actividadesDAO.findById(actividadesDTO.getActividadId()).map(existingActividad -> {
            Actividades actividad = convertToEntity(actividadesDTO);
            Actividades updatedActividad = actividadesDAO.save(actividad);
            ActividadesDTO result = convertToDTO(updatedActividad);
            logger.info("Actividad actualizada con éxito");
            eliminarTodasLasClavesDePagina();
            return result;
        }).orElse(null);
    }

    @Override
    @CacheEvict(value = "actividadById", key = "#id")
    public ActividadesDTO delete(Integer id) {
        return actividadesDAO.findById(id).map(actividad -> {
            actividadesDAO.deleteById(id);
            logger.info("Actividad eliminada con éxito");
            eliminarTodasLasClavesDePagina();
            return convertToDTO(actividad);
        }).orElse(null);
    }

    private void eliminarTodasLasClavesDePagina() {
        String patron = CACHE_KEY_PREFIX + "EVENTO_*_PAGE_IDS_*";
        Set<String> claves = redisTemplate.keys(patron);

        if (claves != null && !claves.isEmpty()) {
            redisTemplate.delete(claves);
            logger.info("{} claves eliminadas que coinciden con el patrón: {}", claves.size(), patron);
        } else {
            logger.info("No se encontraron claves que coincidan con el patrón: {}", patron);
        }
    }
}
