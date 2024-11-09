package com.miapp.modulocontrataciones.Services.Actividades;

import com.miapp.sistemasdistribuidos.dto.ActividadesDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IActividadesService {
    Page<ActividadesDTO> getAllAByEventoID(Pageable pageable, Integer eventoID);
    ActividadesDTO getById(Integer id);
    ActividadesDTO create(ActividadesDTO actividadesDTO);
    ActividadesDTO update(ActividadesDTO actividadesDTO);
    ActividadesDTO delete(Integer id);
}
