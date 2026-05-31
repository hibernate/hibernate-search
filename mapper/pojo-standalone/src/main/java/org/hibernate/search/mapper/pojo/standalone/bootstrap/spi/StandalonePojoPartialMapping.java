/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.bootstrap.spi;

import java.util.Collection;

import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A partial mapping obtained after completing only Phase 1 of bootstrap.
 * <p>
 * Provides access to indexed entity metadata (including index field descriptors)
 * without starting backends or index managers.
 * <p>
 * Must be {@link #close() closed} to release Phase 1 resources.
 */
@Incubating
public interface StandalonePojoPartialMapping extends AutoCloseable {

	Collection<SearchIndexedEntity<?>> allIndexedEntities();

	@Override
	void close();
}
