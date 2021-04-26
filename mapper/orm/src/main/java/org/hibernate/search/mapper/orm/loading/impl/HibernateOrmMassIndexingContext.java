/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.mapper.orm.massindexing.impl.HibernateOrmMassIndexingDocumentProducerInterceptor;
import org.hibernate.search.mapper.orm.massindexing.impl.HibernateOrmMassIndexingIdentifierProducerInterceptor;
import org.hibernate.search.mapper.orm.massindexing.impl.HibernateOrmMassIndexingMappingContext;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContextProvider;
import org.hibernate.search.mapper.pojo.loading.LoadingInterceptor;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingEntityLoadingStrategy;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;
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
	public <T> MassIndexingEntityLoadingStrategy<? super T, HibernateOrmMassIndexingOptions> indexLoadingStrategy(
			PojoRawTypeIdentifier<T> expectedType) {
		LoadingIndexedTypeContext<T> typeContext = typeContextProvider.indexedForExactType( expectedType );
		return typeContext.loadingStrategy();
	}

	@Override
	public List<LoadingInterceptor> identifierInterceptors(HibernateOrmMassIndexingOptions options) {
		return Collections.singletonList( new HibernateOrmMassIndexingIdentifierProducerInterceptor( mappingContext, options ) );
	}

	@Override
	public List<LoadingInterceptor> documentInterceptors(HibernateOrmMassIndexingOptions options) {
		return Collections.singletonList( new HibernateOrmMassIndexingDocumentProducerInterceptor( mappingContext, options ) );
	}

}
