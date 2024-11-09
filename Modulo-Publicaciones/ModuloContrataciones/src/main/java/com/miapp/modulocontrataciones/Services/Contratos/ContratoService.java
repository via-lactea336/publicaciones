package com.miapp.modulocontrataciones.Services.Contratos;

import com.miapp.modulocontrataciones.Daos.ContratoDAO;
import com.miapp.sistemasdistribuidos.dto.ContratoDTO;
import com.miapp.sistemasdistribuidos.entity.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@Transactional
public class ContratoService implements IContratoService {
    private static final Logger logger = LoggerFactory.getLogger(ContratoService.class);
    @Autowired
    private ContratoDAO contratoDAO;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_KEY_PREFIX = "contratoById::";

    private ContratoDTO convertToDTO(Contrato contrato) {
        ContratoDTO contratoDTO = new ContratoDTO();
        contratoDTO.setContratoId(contrato.getContratoId());
        contratoDTO.setPublicacionId(contrato.getPublicacionId().getPublicacionId());
        contratoDTO.setClienteId(contrato.getClienteId().getClienteId());
        contratoDTO.setTrabajadorId(contrato.getTrabajadorId().getTrabajadorId());
        contratoDTO.setFechaContrato(contrato.getFechaContrato());
        contratoDTO.setEstadoId(contrato.getEstadoId().getEstadoId());
        contratoDTO.setPrecio(contrato.getPrecio());
        contratoDTO.setCreatedAt(contrato.getCreatedAt());
        contratoDTO.setUpdatedAt(contrato.getUpdatedAt());
        return contratoDTO;
    }

    private Contrato convertToEntity(ContratoDTO contratoDTO) {
        Contrato contrato = new Contrato();
        contrato.setContratoId(contratoDTO.getContratoId());
        contrato.setFechaContrato(contratoDTO.getFechaContrato());
        contrato.setPrecio(contratoDTO.getPrecio());
        contrato.setCreatedAt(contratoDTO.getCreatedAt());
        contrato.setUpdatedAt(contratoDTO.getUpdatedAt());


        Publicacion publicacion = new Publicacion();
        publicacion.setPublicacionId(contratoDTO.getPublicacionId());
        contrato.setPublicacionId(publicacion);

        Cliente cliente = new Cliente();
        cliente.setClienteId(contratoDTO.getClienteId());
        contrato.setClienteId(cliente);

        Trabajador trabajador = new Trabajador();
        trabajador.setTrabajadorId(contratoDTO.getTrabajadorId());
        contrato.setTrabajadorId(trabajador);

        Estado estado = new Estado();
        estado.setEstadoId(contratoDTO.getEstadoId());
        contrato.setEstadoId(estado);

        return contrato;
    }


    @Override
    public Page<ContratoDTO> getAllContratos(Pageable pageable) {
        try {
            String pageIdsCacheKey = CACHE_KEY_PREFIX + "PAGE_IDS_" + pageable.getPageNumber();
            List<Integer> contratoIds = (List<Integer>) redisTemplate.opsForValue().get(pageIdsCacheKey);
            List<ContratoDTO> contratosCache = new ArrayList<>();

            if (contratoIds != null && !contratoIds.isEmpty()) {
                for (Integer contratoId : contratoIds) {
                    ContratoDTO contratoDTO = (ContratoDTO) redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + contratoId);
                    if (contratoDTO != null) {
                        contratosCache.add(contratoDTO);
                    }
                }
                if (!contratosCache.isEmpty()) {
                    logger.info("Contratos obtenidos desde el cache para la página: " + pageable.getPageNumber());
                    return new PageImpl<>(contratosCache, pageable, contratosCache.size());
                }
            }

            Page<Contrato> contratos = contratoDAO.findAll(pageable);

            if (contratos.hasContent()) {
                List<Integer> nuevosContratoIds = new ArrayList<>();
                List<ContratoDTO> contratoDTOs = new ArrayList<>();

                contratos.forEach(contrato -> {
                    ContratoDTO contratoDTO = convertToDTO(contrato);
                    contratoDTOs.add(contratoDTO);

                    redisTemplate.opsForValue().set(CACHE_KEY_PREFIX + contrato.getContratoId(), contratoDTO, 10, TimeUnit.MINUTES);
                    nuevosContratoIds.add(contrato.getContratoId());
                });

                redisTemplate.opsForValue().set(pageIdsCacheKey, nuevosContratoIds, 10, TimeUnit.MINUTES);

                logger.info("Lista de contratos paginada obtenida correctamente desde la base de datos y almacenada en caché.");
                return new PageImpl<>(contratoDTOs, pageable, contratos.getTotalElements());
            }

            logger.info("No se encontraron contratos en la base de datos.");
            return Page.empty(pageable);

        } catch (Exception e) {
            logger.error("Error al obtener los contratos paginados: ", e);
            throw e;
        }
    }



    @Override
    @Cacheable(value = "contratoById", key = "#id", unless = "#result == null")
    public Optional<ContratoDTO> getContratoById(Integer id) {
        try {
            Optional<Contrato> contrato = contratoDAO.findById(id);
            if (contrato.isEmpty()) {
                logger.error("No se encontró el contrato con el ID: " + id);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato no encontrado");
            }
            Optional<ContratoDTO> result = contrato.map(this::convertToDTO);
            logger.info("Retornando contrato con ID: " + id);
            return result;
        } catch (ResponseStatusException e) {
            logger.error("Error al obtener el contrato: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado al obtener el contrato", e);
            throw e;
        }
    }

    @Override
    @CachePut(value = "contratoById", key = "#result.contratoId")
    public ContratoDTO createContrato(ContratoDTO contratoDTO) {
        try {
            Contrato contrato = convertToEntity(contratoDTO);
            Contrato savedContrato = contratoDAO.save(contrato);
            ContratoDTO result = convertToDTO(savedContrato);
            logger.info("Contrato creado con exito");
            eliminarTodasLasClavesDePagina();
            return result;
        } catch (Exception e) {
            logger.error("Error durante la creacion del contrato", e.getMessage());
            throw e;
        }
    }

    @Override
    @CachePut(value = "contratoById", key = "#id")
    public ContratoDTO updateContrato(Integer id, ContratoDTO contratoDTO) {
        try {
            Optional<Contrato> existingContrato = contratoDAO.findById(id);
            if (existingContrato.isPresent()) {
                Contrato contrato = convertToEntity(contratoDTO);
                contratoDAO.save(contrato);
                ContratoDTO result = convertToDTO(contrato);
                logger.info("Contrato actualizado con éxito", result.toString());
                return result;
            } else {
                logger.warn("No se encontró el contrato con el ID proporcionado");
                return null;
            }
        } catch (Exception e) {
            logger.error("Error al actualizar el contrato ", e.getMessage());
            throw e;
        }
    }


    @Override
    @CacheEvict(value = "contratoById", key = "#id")
    public void deleteContrato(Integer id) {
        try {
            contratoDAO.deleteById(id);
            logger.info("Contrato eliminado con exito");
            eliminarTodasLasClavesDePagina();
        } catch (Exception e) {
            logger.error("Error al eliminar el contrato ", e.getMessage());
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



    @Override
    public Page<ContratoDTO> findByCliente(Integer clienteId, Pageable pageable) {
        try {
            Cliente cliente = new Cliente();
            cliente.setClienteId(clienteId);
            Page<ContratoDTO> result = contratoDAO.findAllByClienteId(cliente, (Pageable) pageable).map(this::convertToDTO);
            logger.info("Retornando contratos filtrados por cliente");
            return result;
        } catch (Exception e) {
            logger.error("Error al obtener los contratos ", e.getMessage());
            throw e;
        }
    }

    @Override
    public Page<ContratoDTO> findByPublicacion(Integer publicacionId, Pageable pageable) {
        try {
            String pageIdsCacheKey = CACHE_KEY_PREFIX + "PUBLICACION_" + publicacionId + "_PAGE_IDS_" + pageable.getPageNumber();
            List<Integer> contratoIds = (List<Integer>) redisTemplate.opsForValue().get(pageIdsCacheKey);
            List<ContratoDTO> contratosCache = new ArrayList<>();
            if (contratoIds != null && !contratoIds.isEmpty()) {
                for (Integer contratoId : contratoIds) {
                    ContratoDTO contratoDTO = (ContratoDTO) redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + contratoId);
                    if (contratoDTO != null) {
                        contratosCache.add(contratoDTO);
                    }
                }
                if (!contratosCache.isEmpty()) {
                    logger.info("Contratos obtenidos desde el cache para la publicación: " + publicacionId + " en la página: " + pageable.getPageNumber());
                    return new PageImpl<>(contratosCache, pageable, contratosCache.size());
                }
            }
            Publicacion publicacion = new Publicacion();
            publicacion.setPublicacionId(publicacionId);
            Page<Contrato> contratos = contratoDAO.findAllByPublicacionId(publicacion, pageable);

            if (contratos.hasContent()) {
                List<Integer> nuevosContratoIds = new ArrayList<>();
                List<ContratoDTO> contratoDTOs = new ArrayList<>();
                contratos.forEach(contrato -> {
                    ContratoDTO contratoDTO = convertToDTO(contrato);
                    contratoDTOs.add(contratoDTO);
                    redisTemplate.opsForValue().set(CACHE_KEY_PREFIX + contrato.getContratoId(), contratoDTO, 60, TimeUnit.SECONDS);
                    nuevosContratoIds.add(contrato.getContratoId());
                });
                redisTemplate.opsForValue().set(pageIdsCacheKey, nuevosContratoIds, 60, TimeUnit.SECONDS);
                logger.info("Contratos filtrados por publicación obtenidos de la base de datos y almacenados en caché para la publicación: " + publicacionId);
                return new PageImpl<>(contratoDTOs, pageable, contratos.getTotalElements());
            }
            logger.info("No se encontraron contratos para la publicación: " + publicacionId);
            return Page.empty(pageable);

        } catch (Exception e) {
            logger.error("Error al obtener los contratos para la publicación: " + publicacionId, e);
            throw e;
        }
    }


    @Override
    public Page<ContratoDTO> findByEstado(Integer estadoId, Pageable pageable) {
        try {
            Estado estado = new Estado();
            estado.setEstadoId(estadoId);
            Page<ContratoDTO> result = contratoDAO.findAllByEstadoId(estado, (Pageable) pageable).map(this::convertToDTO);
            logger.info("Retornando contratos filtrados por estado");
            return result;
        } catch (Exception e) {
            logger.error("Error al obtener los contratos ", e.getMessage());
            throw e;
        }
    }
}
