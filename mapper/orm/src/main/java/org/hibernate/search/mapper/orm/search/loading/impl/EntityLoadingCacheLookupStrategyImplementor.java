/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import org.hibernate.engine.spi.EntityKey;

public interface EntityLoadingCacheLookupStrategyImplementor {

	/**
	 * @param entityKey The key of an entity.
	 * @return The entity, loaded from the cache, or {@code null} if not found.
	 */
	Object lookup(EntityKey entityKey);

}
