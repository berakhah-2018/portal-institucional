package com.portal.adapter.in.rest;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.portal.application.port.in.GetDashboardUseCase;
import com.portal.domain.DashboardSnapshot;
import com.portal.domain.Role;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;

@RestController
public class ReportController {
    private final GetDashboardUseCase getDashboard;

    public ReportController(GetDashboardUseCase getDashboard) {
        this.getDashboard = getDashboard;
    }

    @GetMapping("/api/reports/summary.pdf")
    public ResponseEntity<byte[]> summary(Authentication authentication) throws Exception {
        Role role = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> Role.valueOf(authority.substring(5)))
                .findFirst()
                .orElse(Role.LECTOR);
        DashboardSnapshot snapshot = getDashboard.execute(role);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, output);
        document.open();
        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        document.add(new Paragraph("Portal Institucional", title));
        document.add(new Paragraph("Informe de resumen para perfil " + role));
        document.add(new Paragraph(""));
        document.add(new Paragraph(snapshot.title()));
        document.add(new Paragraph(snapshot.subtitle()));
        document.add(new Paragraph(""));
        for (DashboardSnapshot.Metric metric : snapshot.metrics()) {
            document.add(new Paragraph(metric.label() + ": " + metric.value() + " - " + metric.note()));
        }
        if (snapshot.demoData()) {
            document.add(new Paragraph("\nDatos de demostración; no representan métricas reales."));
        }
        document.close();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=portal-resumen.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(output.toByteArray());
    }
}
