package com.portal.adapter.out.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.portal.application.port.out.ImageStoragePort;
import com.portal.domain.Role;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class GoogleDriveAdapter implements ImageStoragePort {
    private static final String APP_MARKER = "portal-institucional";
    private final RestClient drive = RestClient.builder().baseUrl("https://www.googleapis.com").build();

    @Override
    public StoredImage upload(String accessToken, String fileName, String contentType, byte[] content, Set<Role> visibleTo) {
        Map<String, Object> metadata = Map.of(
                "name", fileName,
                "mimeType", contentType,
                "appProperties", Map.of("portal", APP_MARKER, "visibleTo", visibleTo.stream().map(Enum::name).sorted().reduce((left, right) -> left + "," + right).orElse(""))
        );
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
        HttpHeaders imageHeaders = new HttpHeaders();
        imageHeaders.setContentType(MediaType.parseMediaType(contentType));
        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("metadata", new HttpEntity<>(metadata, jsonHeaders));
        multipart.add("file", new HttpEntity<>(resource, imageHeaders));
        JsonNode response = drive.post()
                .uri("/upload/drive/v3/files?uploadType=multipart&fields=id,name,mimeType,appProperties")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(multipart)
                .retrieve()
                .body(JsonNode.class);
        return toStoredImage(response);
    }

    @Override
    public List<StoredImage> listVisible(String accessToken, Role role) {
        String query = "appProperties has { key='portal' and value='" + APP_MARKER + "' } and trashed = false";
        String uri = UriComponentsBuilder.fromPath("/drive/v3/files")
                .queryParam("q", query)
                .queryParam("fields", "files(id,name,mimeType,appProperties)")
                .queryParam("pageSize", 100)
                .build()
                .encode()
                .toUriString();
        JsonNode response = drive.get()
                .uri(uri)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(JsonNode.class);
        List<StoredImage> images = new ArrayList<>();
        if (response != null) {
            for (JsonNode file : response.path("files")) {
                StoredImage image = toStoredImage(file);
                if (image.contentType() != null && image.contentType().startsWith("image/") && image.visibleTo().contains(role)) {
                    images.add(image);
                }
            }
        }
        return images;
    }

    @Override
    public ImageContent downloadVisible(String accessToken, String fileId, Role role) {
        StoredImage image = listVisible(accessToken, role).stream()
                .filter(candidate -> candidate.id().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new ImageNotFoundException());
        ResponseEntity<byte[]> response = drive.get()
                .uri("/drive/v3/files/{id}?alt=media", fileId)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .toEntity(byte[].class);
        return new ImageContent(response.getBody() == null ? new byte[0] : response.getBody(), image.contentType());
    }

    private StoredImage toStoredImage(JsonNode file) {
        JsonNode properties = file.path("appProperties");
        Set<Role> visibleTo = EnumSet.noneOf(Role.class);
        for (String roleName : properties.path("visibleTo").asText("").split(",")) {
            try {
                visibleTo.add(Role.valueOf(roleName));
            } catch (IllegalArgumentException ignored) { }
        }
        return new StoredImage(
                file.path("id").asText(),
                file.path("name").asText(),
                file.path("mimeType").asText(),
                Set.copyOf(visibleTo)
        );
    }

    public static class ImageNotFoundException extends RuntimeException { }
}
