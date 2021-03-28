/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.mapper.orm.automaticindexing.impl.AutomaticIndexingIndexedTypeContext;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmEntityIdEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmNonEntityIdPropertyEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeIndexedTypeContext;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionIndexedTypeContext;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContextProvider;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.IdentifierMapping;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmEntityLoadingStrategy;

class HibernateOrmIndexedTypeContext<E> extends AbstractHibernateOrmTypeContext<E>
		implements SearchIndexedEntity<E>, HibernateOrmSessionIndexedTypeContext<E>,
				HibernateOrmScopeIndexedTypeContext<E>, AutomaticIndexingIndexedTypeContext {

	private final boolean documentIdIsEntityId;
	private final HibernateOrmEntityLoadingStrategy<? super E> loadingStrategy;
	private final IdentifierMapping<?> identifierMapping;

	private final MappedIndexManager indexManager;

	// Casts are safe because the loading strategy will target either "E" or "? super E", by contract
	@SuppressWarnings("unchecked")
	private HibernateOrmIndexedTypeContext(Builder<E> builder, HibernateOrmSessionTypeContextProvider typeContextContainer,
			SessionFactoryImplementor sessionFactory) {
		super( builder, sessionFactory );

		if ( builder.documentIdSourcePropertyName.equals( entityPersister().getIdentifierPropertyName() ) ) {
			documentIdIsEntityId = true;
			loadingStrategy = (HibernateOrmEntityLoadingStrategy<? super E>) HibernateOrmEntityIdEntityLoadingStrategy.create( sessionFactory,
					typeContextContainer, entityPersister() );
		}
		else {
			// The entity ID is not the property used to generate the document ID
			// We need to use a criteria query to load entities from the document IDs
			documentIdIsEntityId = false;
			loadingStrategy = (HibernateOrmEntityLoadingStrategy<? super E>) HibernateOrmNonEntityIdPropertyEntityLoadingStrategy.create( sessionFactory,
					typeContextContainer, entityPersister(),
					builder.documentIdSourcePropertyName, builder.documentIdSourcePropertyHandle );
		}

		this.identifierMapping = builder.identifierMapping;
		this.indexManager = builder.indexManager;
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

	@Override
	public Object toIndexingPlanProvidedId(Object entityId) {
		if ( documentIdIsEntityId ) {
			return entityId;
		}
		else {
			// The entity ID is not the property used to generate the document ID
			// Return null, meaning the document ID has to be extracted from the entity
			return null;
		}
	}

	@Override
	public IdentifierMapping getIdentifierMapping() {
		return identifierMapping;
	}

	@Override
	public HibernateOrmEntityLoadingStrategy<? super E> loadingStrategy() {
		return loadingStrategy;
	}

	static class Builder<E> extends AbstractBuilder<E> implements PojoIndexedTypeExtendedMappingCollector {

		private String documentIdSourcePropertyName;
		private ValueReadHandle<?> documentIdSourcePropertyHandle;
		private IdentifierMapping<?> identifierMapping;

		private MappedIndexManager indexManager;

		Builder(PojoRawTypeIdentifier<E> typeIdentifier, String jpaEntityName, String hibernateOrmEntityName) {
			super( typeIdentifier, jpaEntityName, hibernateOrmEntityName );
		}

		@Override
		public void documentIdSourceProperty(PojoPropertyModel<?> documentIdSourceProperty) {
			this.documentIdSourcePropertyName = documentIdSourceProperty.name();
			this.documentIdSourcePropertyHandle = documentIdSourceProperty.handle();
		}

		@Override
		public void identifierMapping(IdentifierMapping identifierMapping) {
			this.identifierMapping = identifierMapping;
		}

		@Override
		public void indexManager(MappedIndexManager indexManager) {
			this.indexManager = indexManager;
		}

		public HibernateOrmIndexedTypeContext<E> build(HibernateOrmSessionTypeContextProvider typeContextContainer,
				SessionFactoryImplementor sessionFactory) {
			return new HibernateOrmIndexedTypeContext<>( this, typeContextContainer, sessionFactory );
		}
	}
}
