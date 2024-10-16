package org.jemberai.dataintake.domain;
/*
 * Created by Ashok Kumar Pant
 * Email: asokpant@gmail.com
 * Created on 16/10/2024.
 */

import lombok.Getter;

@Getter
public enum ContentType {
    APPLICATION_STREAM_JSON("application/stream+json"),
    APPLICATION_JSON("application/json"),
    APPLICATION_XHTML_XML("application/xhtml+xml"),
    APPLICATION_XML("application/xml"),
    IMAGE_GIF("image/gif"),
    IMAGE_JPEG("image/jpeg"),
    IMAGE_PNG("image/png"),
    MULTIPART_FORM_DATA("multipart/form-data"),
    MULTIPART_MIXED("multipart/mixed"),
    MULTIPART_RELATED("multipart/related"),
    TEXT_EVENT_STREAM("text/event-stream"),
    TEXT_HTML("text/html"),
    TEXT_MARKDOWN("text/markdown"),
    TEXT_PLAIN("text/plain"),
    TEXT_XML("text/xml"),
    TEXT_CSV("text/csv");
    private final String value;

    ContentType(String value) {
        this.value = value;
    }

}

