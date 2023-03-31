/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.automaticindexing.filter.impl;

import java.util.Set;

import org.hibernate.search.mapper.pojo.automaticindexing.filter.spi.PojoAutomaticIndexingTypeFilter;
import org.hibernate.search.mapper.pojo.automaticindexing.filter.spi.PojoAutomaticIndexingTypeFilterHolder;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public class HibernateOrmAutomaticIndexingTypeFilter implements PojoAutomaticIndexingTypeFilter {

	private final PojoAutomaticIndexingTypeFilterHolder fallback;
	private final Set<PojoRawTypeIdentifier<?>> includes;
	private final Set<PojoRawTypeIdentifier<?>> excludes;

	public HibernateOrmAutomaticIndexingTypeFilter(PojoAutomaticIndexingTypeFilterHolder fallback,
			Set<PojoRawTypeIdentifier<?>> includes,
			Set<PojoRawTypeIdentifier<?>> excludes) {
		this.fallback = fallback;
		this.includes = includes;
		this.excludes = excludes;
	}

	@Override
	public boolean isIncluded(PojoRawTypeIdentifier<?> typeIdentifier) {
		if ( excludes.contains( typeIdentifier ) ) {
			return false;
		}

		if ( includes.contains( typeIdentifier ) ) {
			return true;
		}

		return fallback.filter().isIncluded( typeIdentifier );
	}
}
