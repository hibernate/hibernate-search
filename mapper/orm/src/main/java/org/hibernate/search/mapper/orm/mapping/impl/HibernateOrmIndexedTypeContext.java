/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeIndexedTypeContext;
import org.hibernate.search.mapper.orm.search.loading.impl.EntityLoaderFactory;
import org.hibernate.search.mapper.orm.search.loading.impl.HibernateOrmByIdEntityLoader;
import org.hibernate.search.mapper.orm.search.loading.impl.HibernateOrmCriteriaEntityLoader;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionIndexedTypeContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.spi.IdentifierMapping;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector;

class HibernateOrmIndexedTypeContext<E> extends AbstractHibernateOrmTypeContext<E>
		implements HibernateOrmSessionIndexedTypeContext<E>, HibernateOrmScopeIndexedTypeContext<E> {
	private final String indexName;
	private final boolean documentIdIsEntityId;
	private final EntityLoaderFactory loaderFactory;
	private final IdentifierMapping identifierMapping;

	private HibernateOrmIndexedTypeContext(Builder<E> builder, SessionFactoryImplementor sessionFactory) {
		super( builder.javaClass );

		this.indexName = builder.indexName;

		IdentifiableType<E> indexTypeModel = sessionFactory.getMetamodel().entity( getJavaClass() );
		SingularAttribute<? super E, ?> documentIdSourceProperty =
				indexTypeModel.getSingularAttribute( builder.documentIdSourcePropertyName );
		if ( documentIdSourceProperty.isId() ) {
			documentIdIsEntityId = true;
			loaderFactory = HibernateOrmByIdEntityLoader.factory(
					sessionFactory, getJavaClass()
			);
		}
		else {
			// The entity ID is not the property used to generate the document ID
			// We need to use a criteria query to load entities from the document IDs
			documentIdIsEntityId = false;
			loaderFactory = HibernateOrmCriteriaEntityLoader.factory(
					getJavaClass(), documentIdSourceProperty
			);
		}

		this.identifierMapping = builder.identifierMapping;
	}

	public String getIndexName() {
		return indexName;
	}

	@Override
	public Object toWorkPlanProvidedId(Object entityId) {
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
		// TODO HSEARCH-3349 Add support for customizable database retrieval and object lookup?
		//  See HSearch 5: org.hibernate.search.engine.query.hibernate.impl.EntityLoaderBuilder#getObjectInitializer
		return loaderFactory;
	}

	static class Builder<E> implements PojoIndexedTypeExtendedMappingCollector {
		private final Class<E> javaClass;
		private final String indexName;

		private String documentIdSourcePropertyName;
		private IdentifierMapping identifierMapping;

		Builder(Class<E> javaClass, String indexName) {
			this.javaClass = javaClass;
			this.indexName = indexName;
		}

		@Override
		public void documentIdSourcePropertyName(String documentIdSourcePropertyName) {
			this.documentIdSourcePropertyName = documentIdSourcePropertyName;
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
