/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.search.mapper.orm.massindexing.impl.HibernateOrmMassIndexingDocumentProducerInterceptor;
import org.hibernate.search.mapper.orm.massindexing.impl.HibernateOrmMassIndexingIdentifierProducerInterceptor;
import org.hibernate.search.mapper.orm.massindexing.impl.HibernateOrmMassIndexingMappingContext;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContextProvider;
import org.hibernate.search.mapper.pojo.loading.LoadingInterceptor;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingEntityLoadingStrategy;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;

public final class HibernateOrmMassIndexingContext implements PojoMassIndexingContext<HibernateOrmMassIndexingOptions> {
	private final HibernateOrmSessionTypeContextProvider typeContextProvider;
	private final List<LoadingInterceptor<HibernateOrmMassIndexingOptions>> identifierProducerInterceptors = new ArrayList<>();
	private final List<LoadingInterceptor<HibernateOrmMassIndexingOptions>> documentProducerInterceptors = new ArrayList<>();

	public HibernateOrmMassIndexingContext(HibernateOrmMassIndexingMappingContext mappingContext,
			HibernateOrmSessionTypeContextProvider typeContextContainer) {
		this.typeContextProvider = typeContextContainer;
		identifierProducerInterceptors.add( new HibernateOrmMassIndexingIdentifierProducerInterceptor( mappingContext ) );
		documentProducerInterceptors.add( new HibernateOrmMassIndexingDocumentProducerInterceptor( mappingContext ) );
	}

	@Override
	public <T> MassIndexingEntityLoadingStrategy<? super T, HibernateOrmMassIndexingOptions> indexLoadingStrategy(
			PojoRawTypeIdentifier<T> expectedType) {
		LoadingIndexedTypeContext<T> typeContext = typeContextProvider.indexedForExactType( expectedType );
		return typeContext.loadingStrategy();
	}

	@Override
	public List<LoadingInterceptor<HibernateOrmMassIndexingOptions>> identifierInterceptors() {
		return identifierProducerInterceptors;
	}

	@Override
	public List<LoadingInterceptor<HibernateOrmMassIndexingOptions>> documentInterceptors() {
		return documentProducerInterceptors;
	}

}
