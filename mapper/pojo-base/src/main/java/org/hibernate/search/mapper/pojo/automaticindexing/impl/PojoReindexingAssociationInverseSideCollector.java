/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

/**
 * A collector of entities to be reindexed.
 * <p>
 * Used by {@link PojoImplicitReindexingResolver} to return the resolved entities.
 */
public interface PojoReindexingAssociationInverseSideCollector {

	void updateBecauseOfContainedAssociation(PojoRawTypeIdentifier<?> typeIdentifier, Object containingEntity,
			int dirtyAssociationPathOrdinal);

}
