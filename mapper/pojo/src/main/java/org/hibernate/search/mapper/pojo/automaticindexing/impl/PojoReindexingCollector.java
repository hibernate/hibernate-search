/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.impl;

/**
 * A collector of entities to be reindexed.
 * <p>
 * Used by {@link PojoImplicitReindexingResolver} to return the resolved entities.
 */
public interface PojoReindexingCollector {

	void markForReindexing(Object dirtyEntity);

}
