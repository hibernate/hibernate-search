/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.loading.impl;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.CompositeIdOrder;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.IdOrder;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.SingularIdOrder;
import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchEntityLoader;
import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchEntityLoadingOptions;
import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchEntitySink;
import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchIdentifierLoader;
import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchIdentifierLoadingOptions;
import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchLoadingTypeContext;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmLoadingTypeContext;

public class BatchCoreDefaultHibernateOrmBatchLoadingStrategy<E, I> implements HibernateOrmBatchLoadingStrategy<E, I> {

	private final IdOrder idOrder;

	public BatchCoreDefaultHibernateOrmBatchLoadingStrategy(HibernateOrmLoadingTypeContext<E> type) {
		EntityIdentifierMapping identifierMapping = type.entityMappingType().getIdentifierMapping();
		if ( identifierMapping.getPartMappingType() instanceof EmbeddableMappingType ) {
			idOrder = new CompositeIdOrder<>( type );
		}
		else {
			idOrder = new SingularIdOrder<>( type );
		}
	}

	@Override
	public HibernateOrmBatchIdentifierLoader createIdentifierLoader(HibernateOrmBatchLoadingTypeContext<E> typeContext,
			HibernateOrmBatchIdentifierLoadingOptions options) {
		return new BatchCoreDefaultHibernateOrmBatchIdentifierLoader<>( typeContext, options, idOrder );
	}

	@Override
	public HibernateOrmBatchEntityLoader createEntityLoader(HibernateOrmBatchLoadingTypeContext<E> typeContext,
			HibernateOrmBatchEntitySink<E> sink, HibernateOrmBatchEntityLoadingOptions options) {
		return new BatchCoreDefaultHibernateOrmBatchEntityLoader<>( typeContext, sink, options );
	}
}
