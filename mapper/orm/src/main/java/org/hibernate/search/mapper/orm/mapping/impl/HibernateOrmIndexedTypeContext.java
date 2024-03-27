/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.search.projection.spi.ProjectionMappedTypeContext;
import org.hibernate.search.mapper.orm.automaticindexing.impl.AutomaticIndexingIndexedTypeContext;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContextProvider;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

class HibernateOrmIndexedTypeContext<E> extends AbstractHibernateOrmTypeContext<E>
		implements ProjectionMappedTypeContext,
		SearchIndexedEntity<E>, AutomaticIndexingIndexedTypeContext {

	private final MappedIndexManager indexManager;

	private HibernateOrmIndexedTypeContext(Builder<E> builder, PojoLoadingTypeContext<E> delegate,
			SessionFactoryImplementor sessionFactory) {
		super( builder, delegate, sessionFactory );
		this.indexManager = builder.indexManager;
	}

	@Override
	public String name() {
		return jpaEntityName();
	}

	@Override
	public boolean loadingAvailable() {
		return true;
	}

	@Override
	public String jpaName() {
		return jpaEntityName();
	}

	@Override
	public Class<E> javaClass() {
		return typeIdentifier().javaClass();
	}

	@Override
	public IndexManager indexManager() {
		return indexManager.toAPI();
	}

	static class Builder<E> extends AbstractHibernateOrmTypeContext.Builder<E>
			implements PojoIndexedTypeExtendedMappingCollector {
		private MappedIndexManager indexManager;

		Builder(PojoRawTypeModel<E> typeModel, PersistentClass persistentClass) {
			super( typeModel, persistentClass );
		}

		@Override
		public void indexManager(MappedIndexManager indexManager) {
			this.indexManager = indexManager;
		}

		public HibernateOrmIndexedTypeContext<E> build(PojoLoadingTypeContextProvider delegateProvider,
				SessionFactoryImplementor sessionFactory) {
			return new HibernateOrmIndexedTypeContext<>( this, delegateProvider.forExactType( typeIdentifier ),
					sessionFactory );
		}
	}
}
