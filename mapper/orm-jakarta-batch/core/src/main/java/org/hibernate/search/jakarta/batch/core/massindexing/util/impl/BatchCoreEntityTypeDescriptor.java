/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.util.impl;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.jakarta.batch.core.massindexing.loading.impl.BatchCoreDefaultHibernateOrmBatchLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchLoadingTypeContext;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmLoadingTypeContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public class BatchCoreEntityTypeDescriptor<E, I> implements HibernateOrmBatchLoadingTypeContext<E> {

	public static <E> BatchCoreEntityTypeDescriptor<E, ?> create(SessionFactoryImplementor sessionFactory,
			HibernateOrmLoadingTypeContext<E> type) {
		HibernateOrmBatchLoadingStrategy<E, ?> batchLoadingStrategy = type.batchLoadingStrategy();
		if ( batchLoadingStrategy == null ) {
			batchLoadingStrategy = new BatchCoreDefaultHibernateOrmBatchLoadingStrategy<>( type );
		}
		return new BatchCoreEntityTypeDescriptor<>( type, batchLoadingStrategy );
	}

	private final HibernateOrmLoadingTypeContext<E> delegate;
	private final HibernateOrmBatchLoadingStrategy<E, I> batchLoadingStrategy;

	public BatchCoreEntityTypeDescriptor(HibernateOrmLoadingTypeContext<E> delegate,
			HibernateOrmBatchLoadingStrategy<E, I> batchLoadingStrategy) {
		this.delegate = delegate;
		this.batchLoadingStrategy = batchLoadingStrategy;
	}

	public PojoRawTypeIdentifier<E> typeIdentifier() {
		return delegate.typeIdentifier();
	}

	@Override
	public Class<E> javaClass() {
		return delegate.typeIdentifier().javaClass();
	}

	@Override
	public String jpaEntityName() {
		return delegate.jpaEntityName();
	}

	@Override
	public String uniquePropertyName() {
		return delegate.uniquePropertyName();
	}

	public HibernateOrmBatchLoadingStrategy<E, I> batchLoadingStrategy() {
		return batchLoadingStrategy;
	}

}
