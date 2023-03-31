/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.automaticindexing.filter.impl;

import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMapping;
import org.hibernate.search.mapper.pojo.automaticindexing.filter.PojoAutomaticIndexingTypeFilterConfigurer;
import org.hibernate.search.mapper.pojo.automaticindexing.filter.spi.PojoAutomaticIndexingTypeFilterHolder;

public final class HibernateOrmApplicationAutomaticIndexingTypeFilter {

	private static final PojoAutomaticIndexingTypeFilterHolder INDEXING_TYPE_FILTER_HOLDER = new PojoAutomaticIndexingTypeFilterHolder();
	private static final PojoAutomaticIndexingTypeFilterHolder INCLUDE_ALL_INDEXING_TYPE_FILTER_HOLDER = new PojoAutomaticIndexingTypeFilterHolder();

	private HibernateOrmApplicationAutomaticIndexingTypeFilter() {
	}

	public static void configureFilter(HibernateOrmMapping mapping,
			PojoAutomaticIndexingTypeFilterConfigurer configurer) {

		HibernateOrmAutomaticIndexingTypeFilterContext context = new HibernateOrmAutomaticIndexingTypeFilterContext(
				mapping.typeContextProvider() );
		configurer.configure( context );
		INDEXING_TYPE_FILTER_HOLDER.filter( context.createFilter( INCLUDE_ALL_INDEXING_TYPE_FILTER_HOLDER ) );
	}

	public static PojoAutomaticIndexingTypeFilterHolder applicationFilter() {
		return INDEXING_TYPE_FILTER_HOLDER;
	}
}
