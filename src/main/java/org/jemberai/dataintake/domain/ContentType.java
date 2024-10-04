package org.jemberai.dataintake.domain;
/*
 * Created by Ashok Kumar Pant
 * Email: asokpant@gmail.com
 * Created on 03/10/2024.
 */

import lombok.Getter;

@Getter
public enum ContentType {
    JSON("application/json"),
    XML("application/xml"),
    TEXT("text/plain"),
    PDF("application/pdf"),
    CSV("text/csv");

    private final String value;

    ContentType(String value) {
        this.value = value;
    }

}
