package com.portal.application.port.out;

import com.portal.domain.Role;

import java.util.List;
import java.util.Set;

public interface ImageStoragePort {
    StoredImage upload(String accessToken, String fileName, String contentType, byte[] content, Set<Role> visibleTo);
    List<StoredImage> listVisible(String accessToken, Role role);
    ImageContent downloadVisible(String accessToken, String fileId, Role role);

    record StoredImage(String id, String name, String contentType, Set<Role> visibleTo) { }
    record ImageContent(byte[] content, String contentType) { }
}
