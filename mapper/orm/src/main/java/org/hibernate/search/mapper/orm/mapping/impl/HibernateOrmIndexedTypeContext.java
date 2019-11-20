/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import javax.persistence.metamodel.EntityType;
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
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

class HibernateOrmIndexedTypeContext<E> extends AbstractHibernateOrmTypeContext<E>
		implements HibernateOrmSessionIndexedTypeContext<E>, HibernateOrmScopeIndexedTypeContext<E> {
	private final String indexName;
	private final EntityTypeDescriptor<E> entityType;
	private final boolean documentIdIsEntityId;
	private final EntityLoaderFactory loaderFactory;
	private final IdentifierMapping identifierMapping;

	@SuppressWarnings("unchecked")
	private HibernateOrmIndexedTypeContext(Builder<E> builder, SessionFactoryImplementor sessionFactory) {
		super( builder.javaClass, builder.entityName );

		this.indexName = builder.indexName;

		this.entityType = (EntityTypeDescriptor<E>) getEntityTypeByJpaEntityName( sessionFactory, getEntityName() );
		SingularAttribute<? super E, ?> documentIdSourceAttribute =
				entityType.getSingularAttribute( builder.documentIdSourcePropertyName );
		if ( documentIdSourceAttribute.isId() ) {
			documentIdIsEntityId = true;
			loaderFactory = HibernateOrmByIdEntityLoader.factory(
					sessionFactory, entityType
			);
		}
		else {
			// The entity ID is not the property used to generate the document ID
			// We need to use a criteria query to load entities from the document IDs
			documentIdIsEntityId = false;
			loaderFactory = HibernateOrmCriteriaEntityLoader.factory(
					entityType, documentIdSourceAttribute, builder.documentIdSourcePropertyHandle
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

	@Override
	public EntityTypeDescriptor<E> getEntityType() {
		return entityType;
	}

	private static EntityTypeDescriptor<?> getEntityTypeByJpaEntityName(
			SessionFactoryImplementor sessionFactory, String jpaEntityName) {
		// This is ugly, but there is no other way to get the entity type from its JPA entity name...
		for ( EntityType<?> entity : sessionFactory.getMetamodel().getEntities() ) {
			if ( jpaEntityName.equals( entity.getName() ) ) {
				return (EntityTypeDescriptor<?>) entity;
			}
		}
		throw new AssertionFailure(
				"Could not find the entity type with name '" + jpaEntityName + "'."
				+ " There is a bug in Hibernate Search, please report it."
		);
	}

	static class Builder<E> implements PojoIndexedTypeExtendedMappingCollector {

		private final Class<E> javaClass;
		private final String entityName;
		private final String indexName;

		private String documentIdSourcePropertyName;
		private ValueReadHandle<?> documentIdSourcePropertyHandle;
		private IdentifierMapping identifierMapping;

		Builder(Class<E> javaClass, String entityName, String indexName) {
			this.javaClass = javaClass;
			this.entityName = entityName;
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
