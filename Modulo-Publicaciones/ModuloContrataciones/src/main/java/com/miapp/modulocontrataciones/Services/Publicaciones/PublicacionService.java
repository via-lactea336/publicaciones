package com.miapp.modulocontrataciones.Services.Publicaciones;
import com.miapp.modulocontrataciones.Daos.PublicacionDAO;
import com.miapp.sistemasdistribuidos.dto.PublicacionDTO;
import com.miapp.sistemasdistribuidos.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.TimeUnit;
@Service
@Transactional
public class PublicacionService implements IPublicacionService {
    private static final Logger logger = LoggerFactory.getLogger(PublicacionService.class);

    @Autowired
    private PublicacionDAO publicacionDAO;
    @Value("${timeoutMinutes}")
    private long timeoutMinutes;


    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_KEY_PREFIX = "publicacionById::";

    private PublicacionDTO convertToDTO(Publicacion publicacion) {
        PublicacionDTO PublicacionDTO = new PublicacionDTO();
        PublicacionDTO.setPublicacionId(publicacion.getPublicacionId());
        PublicacionDTO.setPrecio(publicacion.getPrecio());
        PublicacionDTO.setDescripcion(publicacion.getDescripcion());
        PublicacionDTO.setCreatedAt(publicacion.getCreatedAt());
        PublicacionDTO.setUpdatedAt(publicacion.getUpdatedAt());
        PublicacionDTO.setCategoriaId(publicacion.getCategoriaId().getCategoriaId());
        PublicacionDTO.setImagen(publicacion.getImagen());
        PublicacionDTO.setTitulo(publicacion.getTitulo());
        PublicacionDTO.setTipoDePrecioId(publicacion.getTipoDePrecioId().getTipoDePrecioId());
        PublicacionDTO.setTrabajadorId(publicacion.getTrabajadorId().getTrabajadorId());
        return PublicacionDTO;
    }

    private Publicacion convertToEntity(PublicacionDTO publicacionDTO) {
        Publicacion publicacion = new Publicacion();
        publicacion.setPublicacionId(publicacionDTO.getPublicacionId());
        publicacion.setPrecio(publicacionDTO.getPrecio());
        publicacion.setDescripcion(publicacionDTO.getDescripcion());
        publicacion.setCreatedAt(publicacionDTO.getCreatedAt());
        publicacion.setUpdatedAt(publicacionDTO.getUpdatedAt());
        publicacion.setImagen(publicacionDTO.getImagen());
        publicacion.setTitulo(publicacionDTO.getTitulo());

        Trabajador trabajador = new Trabajador();
        trabajador.setTrabajadorId(publicacionDTO.getTrabajadorId());
        publicacion.setTrabajadorId(trabajador);

        Categoria categoria = new Categoria();
        categoria.setCategoriaId(publicacionDTO.getCategoriaId());
        publicacion.setCategoriaId(categoria);

        TipoDePrecio tipoDePrecio = new TipoDePrecio();
        tipoDePrecio.setTipoDePrecioId(publicacionDTO.getTipoDePrecioId());
        publicacion.setTipoDePrecioId(tipoDePrecio);
        return publicacion;
    }


    @Override
    public Page<PublicacionDTO> findAllPublicacion(Pageable pageable) {
        try {
//            String pageIdsCacheKey = CACHE_KEY_PREFIX + "PAGE_IDS_" + pageable.getPageNumber();
//            List<Integer> publicacionIds = (List<Integer>) redisTemplate.opsForValue().get(pageIdsCacheKey);
            List<PublicacionDTO> publicacionesCache = new ArrayList<>();
            long timeoutMinutes = 10; // Tiempo de expiración en minutos

            /*if (publicacionIds != null && !publicacionIds.isEmpty()) {
                // Intentamos obtener las publicaciones desde la caché
                for (Integer publicacionId : publicacionIds) {
                    PublicacionDTO publicacionDTO = (PublicacionDTO) redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + publicacionId);
                    if (publicacionDTO != null) {
                        publicacionesCache.add(publicacionDTO);
                    }
                }*/
                // Si tenemos publicaciones suficientes en caché, devolvemos desde la caché
                /*if (publicacionesCache.size() == pageable.getPageSize()) {
                    logger.info("Publicaciones obtenidas desde el cache para la página: " + pageable.getPageNumber());
                    return new PageImpl<>(publicacionesCache, pageable, publicacionesCache.size());
                }
            }*/

            // Si no encontramos suficientes publicaciones en caché, cargamos desde la base de datos
            Page<Publicacion> publicaciones = publicacionDAO.findAll(pageable);

            if (publicaciones.hasContent()) {
                List<Integer> nuevosPublicacionIds = new ArrayList<>();
                List<PublicacionDTO> publicacionDTOs = new ArrayList<>();

                publicaciones.forEach(publicacion -> {
                    PublicacionDTO publicacionDTO = convertToDTO(publicacion);
                    publicacionDTOs.add(publicacionDTO);

                    // Almacenar cada publicación en Redis con expiración
                    redisTemplate.opsForValue().set(
                            CACHE_KEY_PREFIX + publicacion.getPublicacionId(),
                            publicacionDTO,
                            timeoutMinutes,
                            TimeUnit.MINUTES
                    );

                    nuevosPublicacionIds.add(publicacion.getPublicacionId());
                });

                // Almacenar la lista de IDs de publicaciones en Redis
//                redisTemplate.opsForValue().set(pageIdsCacheKey, nuevosPublicacionIds, timeoutMinutes, TimeUnit.MINUTES);

                logger.info("Lista de publicaciones paginada obtenida correctamente desde la base de datos y almacenada en caché.");
                return new PageImpl<>(publicacionDTOs, pageable, publicaciones.getTotalElements());
            }

            logger.info("No se encontraron publicaciones en la base de datos.");
            return Page.empty(pageable);

        } catch (Exception e) {
            logger.error("Error al obtener las publicaciones paginadas: ", e);
            throw e;
        }
    }


    @Override
//    @Cacheable(value = "publicacionById", key = "#id", unless = "#result == null")
    public Optional<PublicacionDTO> findById(Integer id) {
        try {
            Optional<Publicacion> publicacion = publicacionDAO.findById(id);
            if (publicacion.isEmpty()) {
                logger.error("No se encontró la publicación con el ID: " + id);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicación no encontrada");
            }
            Optional<PublicacionDTO> result = publicacion.map(this::convertToDTO);
            logger.info("Retornando publicación con ID: " + id);
            return result;
        } catch (ResponseStatusException e) {
            logger.error("Error al obtener la publicación: " + e.getMessage());
            throw e;
        }catch (Exception e) {
            logger.error("Error inesperado al obtener la publicación", e);
            throw e;
        }
    }



    @Override
//    @CachePut(value = "publicacionById", key = "#result.publicacionId")
    public PublicacionDTO createPublicacion(PublicacionDTO publicacionDTO) {
        try {
            Publicacion publicacion = convertToEntity(publicacionDTO);
            Publicacion savedPublicacion = publicacionDAO.save(publicacion);
            PublicacionDTO result = convertToDTO(savedPublicacion);
            logger.info("Exito al crear la publicacion nueva...", result.toString());
//            eliminarTodasLasClavesDePagina();
            return result;
        } catch (Exception e) {
            logger.error("Error al crear la publicacion...", e.getMessage());
            throw e;
        }
    }

    @Override
//    @CachePut(value = "publicacionById", key = "#id")
    public PublicacionDTO updatePublicacion(Integer id, PublicacionDTO publicacionDTO) {
        try {
            Optional<Publicacion> existingPublicacion = publicacionDAO.findById(id);
            if (existingPublicacion.isPresent()) {
                Publicacion publicacion = convertToEntity(publicacionDTO);
                publicacion.setPublicacionId(id); // Mantén el ID correcto
                Publicacion savedPublicacion = publicacionDAO.save(publicacion);
                PublicacionDTO result = convertToDTO(savedPublicacion);
                logger.info("Publicacion actualizada con éxito", result.toString());
                return result;
            } else {
                logger.warn("No se encontró la publicacion con el ID proporcionado");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Publicación no encontrada");
            }
        } catch (Exception e) {
            logger.error("Error al actualizar la publicacion ", e);
            throw e;
        }
    }




    @Override
//    @CacheEvict(value = "contratoById", key = "#id")
    public void deletePublicacion(Integer id) {
        try {
            publicacionDAO.deleteById(id);
            logger.info("Exito al eliminar la publicacion...");
//            eliminarTodasLasClavesDePagina();
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
    public Page<PublicacionDTO> findAllByTrabajadorId(Integer trabajadorId, Pageable pageable) {
        try{
            Trabajador trabajador = new Trabajador();
            trabajador.setTrabajadorId(trabajadorId);
            Page<PublicacionDTO> result =  publicacionDAO.findAllByTrabajadorId(trabajador, (org.springframework.data.domain.Pageable) pageable).map(this::convertToDTO);
            return result;
        }catch (Exception e){
            logger.error("Error al obtener la publicacion...", e.getMessage());
            throw e;
        }
    }

    @Override
    public Page<PublicacionDTO> findAllByCategoriaId(Integer categoriaId, Pageable pageable) {
        try{
            Categoria categoria = new Categoria();
            categoria.setCategoriaId(categoriaId);
            Page<PublicacionDTO> result =  publicacionDAO.findAllByCategoriaId(categoria, (org.springframework.data.domain.Pageable) pageable).map(this::convertToDTO);
            return result;
        }catch (Exception e){
            logger.error("Error al obtener la publicacion...", e.getMessage());
            throw e;
        }
    }

    @Override
    public Page<PublicacionDTO> findAllByTipoDePrecioId(Integer tipoDePrecioId, Pageable pageable) {
        try{
            TipoDePrecio tipoDePrecio = new TipoDePrecio();
            tipoDePrecio.setTipoDePrecioId(tipoDePrecioId);
            Page<PublicacionDTO> result =  publicacionDAO.findAllByTipoDePrecioId(tipoDePrecio, (org.springframework.data.domain.Pageable) pageable).map(this::convertToDTO);
            return result;
        }catch (Exception e){
            logger.error("Error al obtener la publicacion...", e.getMessage());
            throw e;
        }
    }

    @Override
    public Page<PublicacionDTO> findAllByNombreCategoria(String nombreCategoria, Pageable pageable) {
        try{
            Categoria categoria = new Categoria();
            categoria.setNombreCategoria(nombreCategoria);
            logger.info(categoria.getNombreCategoria());
            Page<PublicacionDTO> result =  publicacionDAO.findAllByNombreCategoria(categoria.getNombreCategoria(), (org.springframework.data.domain.Pageable) pageable).map(this::convertToDTO);
            logger.info("el resultado :",result.toString());
            return result;
        }catch (Exception e){
            logger.error("Error al obtener la publicacion...", e.getMessage());
            throw e;
        }
    }

    @Override
    public Page<PublicacionDTO> findAllByDescripcionContainingIgnoreCaseOrTituloContainingIgnoreCase(String descripcion, String title, Pageable pageable) {
        try{
            Page<PublicacionDTO> result =  publicacionDAO.findAllByDescripcionContainingIgnoreCaseOrTituloContainingIgnoreCase(descripcion,title, (org.springframework.data.domain.Pageable) pageable).map(this::convertToDTO);
            logger.info("el resultado :",result.toString());
            return result;
        }catch (Exception e){
            logger.error("Error al obtener la publicacion...", e.getMessage());
            throw e;
        }
    }
}
