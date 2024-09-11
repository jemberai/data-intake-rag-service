/*
 *
 *  * Copyright 2023 - 2024 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * https://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.jemberai.dataintake.domain;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;
import org.jemberai.cryptography.model.EncryptedValueDTO;
import org.jemberai.cryptography.provider.EncryptionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Created by jt, Spring Framework Guru.
 */
@Slf4j
@Component
public class EventRecordListener {
    private EncryptionProvider encryptionProvider;

    @Autowired
    public void setEncryptionProvider(@Qualifier("encryptionProviderJPA") EncryptionProvider encryptionProvider) {
        this.encryptionProvider = encryptionProvider;
    }

    @PrePersist
    public void prePersist(EventRecord eventRecord){
        setEncryptedFields(eventRecord);
    }

    @PreUpdate
    public void preUpdate(EventRecord eventRecord){
        setEncryptedFields(eventRecord);
    }

    private static void setSha256(EventRecord eventRecord) {
        if (eventRecord.getData() != null && eventRecord.getSha256() == null) {
            String sha256hex = org.apache.commons.codec.digest.DigestUtils.sha256Hex(eventRecord.getData());
            eventRecord.setSha256(sha256hex);
        }
    }

    private void setEncryptedFields(EventRecord eventRecord) {
        setSha256(eventRecord);

        EncryptedValueDTO dto = encryptionProvider.encrypt(eventRecord.getClientId(), eventRecord.getData());

        eventRecord.setProvider(dto.provider());
        eventRecord.setKeyId(dto.keyId());
        eventRecord.setHmac(dto.hmac());
        eventRecord.setEncryptedValue(dto.encryptedValue());
        eventRecord.setInitializationVector(dto.initializationVector());
    }

    @PostLoad
    public void postLoad(EventRecord eventRecord) {
        eventRecord.setData(encryptionProvider.decrypt(eventRecord.getClientId(),
                new EncryptedValueDTO(eventRecord.getProvider(), eventRecord.getKeyId(), eventRecord.getHmac(),
                        eventRecord.getEncryptedValue(), eventRecord.getInitializationVector())));
    }
}
