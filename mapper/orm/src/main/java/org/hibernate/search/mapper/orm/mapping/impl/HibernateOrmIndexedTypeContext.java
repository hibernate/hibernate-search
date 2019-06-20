/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeIndexedTypeContext;
import org.hibernate.search.mapper.orm.search.loading.impl.HibernateOrmComposableEntityLoader;
import org.hibernate.search.mapper.orm.search.loading.impl.HibernateOrmSingleTypeByIdEntityLoader;
import org.hibernate.search.mapper.orm.search.loading.impl.HibernateOrmSingleTypeCriteriaEntityLoader;
import org.hibernate.search.mapper.orm.search.loading.impl.MutableEntityLoadingOptions;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionIndexedTypeContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.spi.IdentifierMapping;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.orm.common.EntityReference;

class HibernateOrmIndexedTypeContext<E> extends AbstractHibernateOrmTypeContext<E>
		implements HibernateOrmSessionIndexedTypeContext<E>, HibernateOrmScopeIndexedTypeContext<E> {
	private final String indexName;
	private final SingularAttribute<? super E, ?> nonIdDocumentIdSourceProperty;
	private final IdentifierMapping identifierMapping;

	private HibernateOrmIndexedTypeContext(Builder<E> builder, SessionFactory sessionFactory) {
		super( builder.javaClass );

		this.indexName = builder.indexName;

		IdentifiableType<E> indexTypeModel = sessionFactory.getMetamodel().entity( getJavaClass() );
		SingularAttribute<? super E, ?> documentIdSourceProperty =
				indexTypeModel.getSingularAttribute( builder.documentIdSourcePropertyName );
		if ( documentIdSourceProperty.isId() ) {
			nonIdDocumentIdSourceProperty = null;
		}
		else {
			nonIdDocumentIdSourceProperty = documentIdSourceProperty;
		}

		this.identifierMapping = builder.identifierMapping;
	}

	public String getIndexName() {
		return indexName;
	}

	@Override
	public Object toWorkPlanProvidedId(Object entityId) {
		if ( nonIdDocumentIdSourceProperty != null ) {
			// The entity ID is not the property used to generate the document ID
			// Return null, meaning the document ID has to be extracted from the entity
			return null;
		}
		else {
			return entityId;
		}
	}

	@Override
	public IdentifierMapping getIdentifierMapping() {
		return identifierMapping;
	}

	@Override
	public HibernateOrmComposableEntityLoader<EntityReference, E> createLoader(Session session,
			MutableEntityLoadingOptions mutableLoadingOptions) {
		// TODO HSEARCH-3349 Add support for other types of database retrieval and object lookup?
		//  See HSearch 5: org.hibernate.search.engine.query.hibernate.impl.EntityLoaderBuilder#getObjectInitializer

		if ( nonIdDocumentIdSourceProperty != null ) {
			// The entity ID is not the property used to generate the document ID
			// We need to use a criteria query to load entities from the document IDs
			return new HibernateOrmSingleTypeCriteriaEntityLoader<>(
					session,
					getJavaClass(),
					nonIdDocumentIdSourceProperty,
					mutableLoadingOptions
			);
		}
		else {
			return new HibernateOrmSingleTypeByIdEntityLoader<>(
					session,
					getJavaClass(),
					mutableLoadingOptions
			);
		}
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

		public HibernateOrmIndexedTypeContext<E> build(SessionFactory sessionFactory) {
			return new HibernateOrmIndexedTypeContext<>( this, sessionFactory );
		}
	}
}
