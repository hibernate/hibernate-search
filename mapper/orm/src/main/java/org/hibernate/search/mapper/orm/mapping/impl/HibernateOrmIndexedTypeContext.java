/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.mapper.orm.mapping.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeIndexedTypeContext;
import org.hibernate.search.mapper.orm.search.loading.impl.EntityLoaderFactory;
import org.hibernate.search.mapper.orm.search.loading.impl.HibernateOrmByIdEntityLoader;
import org.hibernate.search.mapper.orm.search.loading.impl.HibernateOrmCriteriaEntityLoader;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionIndexedTypeContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.IdentifierMapping;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

class HibernateOrmIndexedTypeContext<E> extends AbstractHibernateOrmTypeContext<E>
		implements SearchIndexedEntity, HibernateOrmSessionIndexedTypeContext<E>, HibernateOrmScopeIndexedTypeContext<E> {

	private final boolean documentIdIsEntityId;
	private final EntityLoaderFactory loaderFactory;
	private final IdentifierMapping identifierMapping;

	private final MappedIndexManager indexManager;

	private HibernateOrmIndexedTypeContext(Builder<E> builder, SessionFactoryImplementor sessionFactory) {
		super( sessionFactory, builder.typeIdentifier, builder.jpaEntityName, builder.hibernateOrmEntityName );

		if ( entityPersister().getIdentifierPropertyName().equals( builder.documentIdSourcePropertyName ) ) {
			documentIdIsEntityId = true;
			loaderFactory = HibernateOrmByIdEntityLoader.factory(
					sessionFactory, entityPersister()
			);
		}
		else {
			// The entity ID is not the property used to generate the document ID
			// We need to use a criteria query to load entities from the document IDs
			documentIdIsEntityId = false;
			EntityTypeDescriptor<E> typeDescriptor = entityTypeDescriptor();
			SingularAttribute<? super E, ?> documentIdSourceAttribute =
					typeDescriptor.getSingularAttribute( builder.documentIdSourcePropertyName );
			loaderFactory = HibernateOrmCriteriaEntityLoader.factory(
					typeDescriptor, documentIdSourceAttribute, builder.documentIdSourcePropertyHandle
			);
		}

		this.identifierMapping = builder.identifierMapping;
		this.indexManager = builder.indexManager;
	}

	@Override
	public String jpaName() {
		return jpaEntityName();
	}

	@Override
	public Class<?> javaClass() {
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
	public EntityLoaderFactory loaderFactory() {
		return loaderFactory;
	}

	static class Builder<E> implements PojoIndexedTypeExtendedMappingCollector {

		private final PojoRawTypeIdentifier<E> typeIdentifier;
		private final String jpaEntityName;
		private final String hibernateOrmEntityName;

		private String documentIdSourcePropertyName;
		private ValueReadHandle<?> documentIdSourcePropertyHandle;
		private IdentifierMapping identifierMapping;

		private MappedIndexManager indexManager;

		Builder(PojoRawTypeIdentifier<E> typeIdentifier, String jpaEntityName, String hibernateOrmEntityName) {
			this.typeIdentifier = typeIdentifier;
			this.jpaEntityName = jpaEntityName;
			this.hibernateOrmEntityName = hibernateOrmEntityName;
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

		public HibernateOrmIndexedTypeContext<E> build(SessionFactoryImplementor sessionFactory) {
			return new HibernateOrmIndexedTypeContext<>( this, sessionFactory );
		}
	}
}
