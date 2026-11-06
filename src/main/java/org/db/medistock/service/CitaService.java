package org.db.medistock.service;

import org.db.medistock.model.Cita;
import org.db.medistock.model.Usuario;
import org.db.medistock.repository.CitaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class CitaService {

    private final CitaRepository citaRepository;

    public CitaService(CitaRepository citaRepository) {
        this.citaRepository = citaRepository;
    }

    // Obtener Cita

    @Transactional(readOnly = true)
    public Cita obtenerCitaActual(Usuario usuario) {
        List<Cita> citas = citaRepository.findByUsuario(usuario);

        if (!citas.isEmpty()) {
            return citas.get(0);
        }
        return null;
    }

    // Generar Nueva Cita

    @Transactional
    public CitaResultado generarNuevaCita(Usuario usuario) {
        List<Cita> existentes = citaRepository.findByUsuario(usuario);
        if (!existentes.isEmpty() && !existentes.get(0).isCodestado()) {
            return new CitaResultado(existentes.get(0), "Ya tienes un codigo generado");
        }

        Cita cita = new Cita();
        cita.setUsuario(usuario);
        cita.setCodigo(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        cita.setCodestado(false); // false indica que está activo/no expirado
        citaRepository.save(cita);

        return new CitaResultado(cita, "Tu codigo fue generado correctamente");
    }

    // Clase Auxiliar De Mensaje

    public static class CitaResultado {
        private final Cita cita;
        private final String mensaje;

        public CitaResultado(Cita cita, String mensaje) {
            this.cita = cita;
            this.mensaje = mensaje;
        }

        public Cita getCita() {
            return cita;
        }

        public String getMensaje() {
            return mensaje;
        }
    }
}