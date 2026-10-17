package com.portal.adapter.in.rest;

import com.portal.application.port.out.ImageStoragePort;
import com.portal.application.port.out.ImageStoragePort.ImageContent;
import com.portal.application.port.out.ImageStoragePort.StoredImage;
import com.portal.domain.Role;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/images")
public class ImageController {
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private final ImageStoragePort imageStorage;

    public ImageController(ImageStoragePort imageStorage) {
        this.imageStorage = imageStorage;
    }

    @GetMapping
    public List<StoredImage> list(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client,
            Authentication authentication
    ) {
        return imageStorage.listVisible(client.getAccessToken().getTokenValue(), currentRole(authentication));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StoredImage upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("visibleTo") Set<Role> visibleTo,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client,
            Authentication authentication
    ) throws IOException {
        Role uploader = currentRole(authentication);
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("La imagen debe pesar menos de 10 MB y no estar vacía.");
        }
        if (file.getContentType() == null || !ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Formato no admitido. Usa JPEG, PNG o WebP.");
        }
        if (visibleTo.isEmpty() || visibleTo.stream().anyMatch(role -> !uploader.canView(role))) {
            throw new IllegalArgumentException("No puedes asignar visibilidad a perfiles superiores al tuyo.");
        }
        String fileName = file.getOriginalFilename() == null ? "imagen" : file.getOriginalFilename().replaceAll("[\\r\\n\\\\/]+", "_");
        return imageStorage.upload(client.getAccessToken().getTokenValue(), fileName, file.getContentType(), file.getBytes(), visibleTo);
    }

    @GetMapping(value = "/{fileId}/content")
    public ResponseEntity<byte[]> content(
            @PathVariable String fileId,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client,
            Authentication authentication
    ) {
        ImageContent image = imageStorage.downloadVisible(client.getAccessToken().getTokenValue(), fileId, currentRole(authentication));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("imagen", StandardCharsets.UTF_8).build().toString())
                .body(image.content());
    }

    private Role currentRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> Role.valueOf(authority.substring(5)))
                .findFirst()
                .orElse(Role.LECTOR);
    }
}
