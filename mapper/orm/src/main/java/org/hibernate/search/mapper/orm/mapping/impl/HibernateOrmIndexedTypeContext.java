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
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeIndexedTypeContext;
import org.hibernate.search.mapper.orm.search.loading.impl.EntityLoaderFactory;
import org.hibernate.search.mapper.orm.search.loading.impl.HibernateOrmByIdEntityLoader;
import org.hibernate.search.mapper.orm.search.loading.impl.HibernateOrmCriteriaEntityLoader;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionIndexedTypeContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.spi.IdentifierMapping;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

class HibernateOrmIndexedTypeContext<E> extends AbstractHibernateOrmTypeContext<E>
		implements HibernateOrmSessionIndexedTypeContext<E>, HibernateOrmScopeIndexedTypeContext<E> {
	private final String indexName;
	private final boolean documentIdIsEntityId;
	private final EntityLoaderFactory loaderFactory;
	private final IdentifierMapping identifierMapping;

	private HibernateOrmIndexedTypeContext(Builder<E> builder, SessionFactoryImplementor sessionFactory) {
		super( sessionFactory, builder.typeIdentifier, builder.jpaEntityName, builder.hibernateOrmEntityName );

		this.indexName = builder.indexName;

		if ( getEntityPersister().getIdentifierPropertyName().equals( builder.documentIdSourcePropertyName ) ) {
			documentIdIsEntityId = true;
			loaderFactory = HibernateOrmByIdEntityLoader.factory(
					sessionFactory, getEntityPersister()
			);
		}
		else {
			// The entity ID is not the property used to generate the document ID
			// We need to use a criteria query to load entities from the document IDs
			documentIdIsEntityId = false;
			EntityTypeDescriptor<E> typeDescriptor = getEntityTypeDescriptor();
			SingularAttribute<? super E, ?> documentIdSourceAttribute =
					typeDescriptor.getSingularAttribute( builder.documentIdSourcePropertyName );
			loaderFactory = HibernateOrmCriteriaEntityLoader.factory(
					typeDescriptor, documentIdSourceAttribute, builder.documentIdSourcePropertyHandle
			);
		}

		this.identifierMapping = builder.identifierMapping;
	}

	public String getIndexName() {
		return indexName;
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
	public EntityLoaderFactory getLoaderFactory() {
		return loaderFactory;
	}

	static class Builder<E> implements PojoIndexedTypeExtendedMappingCollector {

		private final PojoRawTypeIdentifier<E> typeIdentifier;
		private final String jpaEntityName;
		private final String hibernateOrmEntityName;
		private final String indexName;

		private String documentIdSourcePropertyName;
		private ValueReadHandle<?> documentIdSourcePropertyHandle;
		private IdentifierMapping identifierMapping;

		Builder(PojoRawTypeIdentifier<E> typeIdentifier, String jpaEntityName, String hibernateOrmEntityName,
				String indexName) {
			this.typeIdentifier = typeIdentifier;
			this.jpaEntityName = jpaEntityName;
			this.hibernateOrmEntityName = hibernateOrmEntityName;
			this.indexName = indexName;
		}

		@Override
		public void documentIdSourceProperty(PojoPropertyModel<?> documentIdSourceProperty) {
			this.documentIdSourcePropertyName = documentIdSourceProperty.getName();
			this.documentIdSourcePropertyHandle = documentIdSourceProperty.getHandle();
		}

		@Override
		public void identifierMapping(IdentifierMapping identifierMapping) {
			this.identifierMapping = identifierMapping;
		}

		public HibernateOrmIndexedTypeContext<E> build(SessionFactoryImplementor sessionFactory) {
			return new HibernateOrmIndexedTypeContext<>( this, sessionFactory );
		}
	}
}
