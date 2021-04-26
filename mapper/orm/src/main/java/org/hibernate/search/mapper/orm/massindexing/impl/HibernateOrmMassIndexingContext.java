/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmMassIndexingOptions;
import org.hibernate.search.mapper.orm.loading.impl.LoadingIndexedTypeContext;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContextProvider;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingLoadingStrategy;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public final class HibernateOrmMassIndexingContext implements PojoMassIndexingContext<HibernateOrmMassIndexingOptions> {
	private final HibernateOrmMassIndexingMappingContext mappingContext;
	private final HibernateOrmSessionTypeContextProvider typeContextProvider;

	public HibernateOrmMassIndexingContext(HibernateOrmMassIndexingMappingContext mappingContext,
			HibernateOrmSessionTypeContextProvider typeContextContainer) {
		this.mappingContext = mappingContext;
		this.typeContextProvider = typeContextContainer;
	}

	@Override
	public <T> PojoMassIndexingLoadingStrategy<? super T, ?, HibernateOrmMassIndexingOptions> loadingStrategy(
			PojoRawTypeIdentifier<T> expectedType) {
		LoadingIndexedTypeContext<T> typeContext = typeContextProvider.indexedForExactType( expectedType );
		return new HibernateOrmMassIndexingLoadingStrategy<>( mappingContext, typeContext.loadingStrategy() );
	}

}
