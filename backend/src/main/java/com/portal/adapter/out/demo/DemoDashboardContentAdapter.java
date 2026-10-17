package com.portal.adapter.out.demo;

import com.portal.application.port.out.DashboardContentPort;
import com.portal.domain.DashboardSnapshot;
import com.portal.domain.Role;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DemoDashboardContentAdapter implements DashboardContentPort {
    @Override
    public DashboardSnapshot loadFor(Role role) {
        return switch (role) {
            case ADMIN -> new DashboardSnapshot(role, "Resumen de administración", "Datos de demostración para el equipo administrador.", List.of(
                    new DashboardSnapshot.Metric("Personas activas", "248", "+12 este mes"),
                    new DashboardSnapshot.Metric("Publicaciones", "36", "4 por revisar"),
                    new DashboardSnapshot.Metric("Recursos", "82", "Biblioteca visual")
            ), true);
            case EDITOR -> new DashboardSnapshot(role, "Espacio editorial", "Contenido pendiente para revisión y publicación.", List.of(
                    new DashboardSnapshot.Metric("En edición", "08", "3 actualizadas hoy"),
                    new DashboardSnapshot.Metric("Por publicar", "04", "Próxima: 14:30"),
                    new DashboardSnapshot.Metric("Imágenes", "21", "Biblioteca visual")
            ), true);
            case LECTOR -> new DashboardSnapshot(role, "Información destacada", "Publicaciones y recursos visibles para tu perfil.", List.of(
                    new DashboardSnapshot.Metric("Novedades", "12", "Desde tu última visita"),
                    new DashboardSnapshot.Metric("Recursos", "28", "Disponibles para ti"),
                    new DashboardSnapshot.Metric("Informes", "06", "Este trimestre")
            ), true);
        };
    }
}
