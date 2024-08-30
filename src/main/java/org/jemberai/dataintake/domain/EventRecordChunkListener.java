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

import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.jemberai.cryptography.model.EncryptedValueDTO;
import org.jemberai.cryptography.provider.EncryptionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * We're making an assumption that the chunk entities will be persisted via the parent EventRecord entity.
 * <p>
 * But we need to decrypt the data when we load the chunk entity if loaded outside the context of the parent entity.
 * <p>
 * Created by jt, Spring Framework Guru.
 */
@Slf4j
@Component
public class EventRecordChunkListener {
    private EncryptionProvider encryptionProvider;

    @Autowired
    public void setEncryptionProvider(@Qualifier("encryptionProviderJPA") EncryptionProvider encryptionProvider) {
        this.encryptionProvider = encryptionProvider;
    }

    @PrePersist
    public void prePersist(EventRecordChunk eventRecordChunk){
        setEncryptedFields(eventRecordChunk);
    }

    @PreUpdate
    public void preUpdate(EventRecordChunk eventRecordChunk){
        setEncryptedFields(eventRecordChunk);
    }

    private void setEncryptedFields(EventRecordChunk eventRecordChunk) {
        setSha256(eventRecordChunk);

        EncryptedValueDTO dto = encryptionProvider.encrypt(eventRecordChunk.getEventRecord().getClientId(), eventRecordChunk.getData());

        eventRecordChunk.setProvider(dto.provider());
        eventRecordChunk.setKeyId(dto.keyId());
        eventRecordChunk.setHmac(dto.hmac());
        eventRecordChunk.setEncryptedValue(dto.encryptedValue());
        eventRecordChunk.setInitializationVector(dto.initializationVector());
        eventRecordChunk.setData("encrypted".getBytes());
    }

    @PostUpdate
    void postUpdate(EventRecordChunk eventRecordChunk) {
        decryptData(eventRecordChunk);
    }

    @PostPersist
    void postPersist(EventRecordChunk eventRecordChunk) {
        decryptData(eventRecordChunk);
    }

    @PostLoad
    public void postLoad(EventRecordChunk eventRecordChunk) {
        decryptData(eventRecordChunk);
    }

    private void decryptData(EventRecordChunk eventRecordChunk) {
        eventRecordChunk.setData(encryptionProvider.decrypt(eventRecordChunk.getEventRecord().getClientId(),
                new EncryptedValueDTO(eventRecordChunk.getProvider(), eventRecordChunk.getKeyId(), eventRecordChunk.getHmac(),
                        eventRecordChunk.getEncryptedValue(), eventRecordChunk.getInitializationVector())));
    }

    private static void setSha256(EventRecordChunk eventRecordChunk) {
        if (eventRecordChunk.getData() != null && eventRecordChunk.getSha256() == null) {
            String sha256hex = org.apache.commons.codec.digest.DigestUtils.sha256Hex(eventRecordChunk.getData());
            eventRecordChunk.setSha256(sha256hex);
        }
    }
}
