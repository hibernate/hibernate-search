/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.impl;

import org.hibernate.engine.spi.EntityKey;

interface EntityLoadingCacheLookupStrategyImplementor {

	/**
	 * @param entityKey The key of an entity.
	 * @return The entity, loaded from the cache, or {@code null} if not found.
	 */
	Object lookup(EntityKey entityKey);

}
