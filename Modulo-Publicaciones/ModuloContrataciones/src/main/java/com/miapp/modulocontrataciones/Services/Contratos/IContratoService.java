package com.miapp.modulocontrataciones.Services.Contratos;

import com.miapp.sistemasdistribuidos.dto.ContratoDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface IContratoService {
    Optional<ContratoDTO> getContratoById(Integer id);
    ContratoDTO createContrato(ContratoDTO contratoDTO);
    ContratoDTO updateContrato(Integer id, ContratoDTO contratoDTO);
    void deleteContrato(Integer id);

    Page<ContratoDTO> getAllContratos(Pageable pageable);
    Page<ContratoDTO> findByCliente(Integer clienteId, Pageable pageable);
    Page<ContratoDTO> findByPublicacion(Integer publicacionId, Pageable pageable);
    Page<ContratoDTO> findByEstado(Integer estadoId, Pageable pageable);

}
