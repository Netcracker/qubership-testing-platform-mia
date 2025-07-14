/*
 *  Copyright 2024-2025 NetCracker Technology Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.qubership.atp.mia.service.history.impl;

import java.util.List;
import java.util.UUID;

import org.javers.core.Javers;
import org.javers.core.commit.CommitId;
import org.javers.repository.jql.JqlQuery;
import org.javers.repository.jql.QueryBuilder;
import org.javers.shadow.Shadow;
import org.qubership.atp.mia.exceptions.history.MiaHistoryRevisionRestoreException;
import org.qubership.atp.mia.model.DateAuditorEntity;
import org.qubership.atp.mia.service.history.EntityHistoryService;
import org.qubership.atp.mia.service.history.RestoreHistoryService;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractRestoreHistoryService<E extends DateAuditorEntity> implements RestoreHistoryService {

    protected final Javers javers;
    protected final EntityHistoryService<E> entityHistoryService;
    private final ValidateReferenceExistsService validateReferenceExistsService;
    protected final AbstractRestoreMapper modelMapper;

    /**
     * Restores the object to a state defined by revision number.
     *
     * @param id         of object being restored.
     * @param revisionId revision number to restore.
     */
    public Object restoreToRevision(UUID id, long revisionId) {

        JqlQuery query = QueryBuilder.byInstanceId(id, getEntityClass())
                .withVersion(revisionId)
                .withScopeDeepPlus()
                .build();

        E actualObject = getObject(id);

        validateReferenceExistsService.validateEntity(actualObject);

        List<Shadow<E>> shadows = javers.findShadows(query);

        if (CollectionUtils.isEmpty(shadows)) {
            log.error("No shadows found for entity '{}' with revision='{}' and uuid='{}'",
                    getItemType(), revisionId, id);

            throw new MiaHistoryRevisionRestoreException();
        }
        Shadow<E> objectShadow = shadows.iterator().next();

        Object restoredObject = restoreValues(objectShadow, actualObject);
        return saveRestoredObject((E) restoredObject);
    }

    /**
     * Restores the object to a state defined by commit id.
     *
     * @param id         of object being restored.
     * @param commitId commit id to restore.
     */
    public Object restoreToCommit(UUID id, CommitId commitId) {

        JqlQuery query = QueryBuilder.byInstanceId(id, getEntityClass())
                .toCommitId(commitId)
                .build();

        E actualObject = getObject(id);

        validateReferenceExistsService.validateEntity(actualObject);

        List<Shadow<E>> shadows = javers.findShadows(query);

        if (CollectionUtils.isEmpty(shadows)) {
            log.warn("No shadows found for entity '{}' with CommitId='{}' and uuid='{}'",
                    getItemType(), commitId, id);
        } else {
            Shadow<E> objectShadow = shadows.iterator().next();

            Object restoredObject = restoreValues(objectShadow, actualObject);
            return saveRestoredObject((E) restoredObject);
        }
       return null;
    }

    protected Object restoreValues(Shadow<E> shadow, E actualObject) {

        E snapshot = shadow.get();
        if (!actualObject.getClass().equals(snapshot.getClass())) {
            throw new MiaHistoryRevisionRestoreException();
        }
        copyValues(snapshot, actualObject);
        return actualObject;
    }

    public E getObject(UUID id) {
        return entityHistoryService.get(id);
    }

    public E saveRestoredObject(E object) {
        return entityHistoryService.restore(object);
    }

    protected abstract void copyValues(E shadow, E actualObject);
}
