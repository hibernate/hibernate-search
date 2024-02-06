/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.search.mapper.orm.event.impl.HibernateOrmListenerTypeContext;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmEntityIdEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmNonEntityIdPropertyEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.spi.LoadingTypeContext;
import org.hibernate.search.mapper.orm.model.impl.DocumentIdSourceProperty;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContext;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeContext;

abstract class AbstractHibernateOrmTypeContext<E>
		implements PojoTypeContext<E>, HibernateOrmListenerTypeContext, HibernateOrmSessionTypeContext<E>,
		LoadingTypeContext<E> {

	private final PojoRawTypeIdentifier<E> typeIdentifier;
	private final String jpaEntityName;
	private final EntityMappingType entityMappingType;
	private final boolean documentIdIsEntityId;
	private final HibernateOrmEntityLoadingStrategy<? super E, ?> loadingStrategy;
	private final PojoPathFilter dirtyFilter;
	private final PojoPathFilter dirtyContainingAssociationFilter;
	private final List<PojoRawTypeIdentifier<? super E>> ascendingSuperTypes;

	// Casts are safe because the loading strategy will target either "E" or "? super E", by contract
	@SuppressWarnings("unchecked")
	AbstractHibernateOrmTypeContext(AbstractBuilder<E> builder, SessionFactoryImplementor sessionFactory) {
		this.typeIdentifier = builder.typeIdentifier;
		this.jpaEntityName = builder.jpaEntityName;
		MappingMetamodel metamodel = sessionFactory.getMappingMetamodel();
		this.entityMappingType = metamodel.getEntityDescriptor( builder.hibernateOrmEntityName );
		this.ascendingSuperTypes = builder.ascendingSuperTypes;
		if ( builder.documentIdSourceProperty != null ) {
			var idProperty = builder.persistentClass.getIdentifierProperty();
			if ( idProperty != null && builder.documentIdSourceProperty.name.equals( idProperty.getName() ) ) {
				documentIdIsEntityId = true;
				loadingStrategy = (HibernateOrmEntityLoadingStrategy<? super E, ?>) HibernateOrmEntityIdEntityLoadingStrategy
						.create( builder.persistentClass );
			}
			else {
				// The entity ID is not the property used to generate the document ID
				// We need to use a criteria query to load entities from the document IDs
				documentIdIsEntityId = false;
				loadingStrategy = (HibernateOrmEntityLoadingStrategy<? super E,
						?>) HibernateOrmNonEntityIdPropertyEntityLoadingStrategy.create( builder.persistentClass,
								builder.documentIdSourceProperty );
			}
		}
		else {
			// Can only happen for contained types, which may not be loadable.
			documentIdIsEntityId = false;
			loadingStrategy = null;
		}
		this.dirtyFilter = builder.dirtyFilter;
		this.dirtyContainingAssociationFilter = builder.dirtyContainingAssociationFilter;
	}

	@Override
	public String toString() {
		return typeIdentifier().toString();
	}

	@Override
	public PojoRawTypeIdentifier<E> typeIdentifier() {
		return typeIdentifier;
	}

	@Override
	public String jpaEntityName() {
		return jpaEntityName;
	}

	public String hibernateOrmEntityName() {
		return entityMappingType.getEntityName();
	}

	@Override
	public EntityMappingType entityMappingType() {
		return entityMappingType;
	}

	@Override
	public HibernateOrmEntityLoadingStrategy<? super E, ?> loadingStrategy() {
		return loadingStrategy;
	}

	@Override
	public List<PojoRawTypeIdentifier<? super E>> ascendingSuperTypes() {
		return ascendingSuperTypes;
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
	public PojoPathFilter dirtyFilter() {
		return dirtyFilter;
	}

	@Override
	public PojoPathFilter dirtyContainingAssociationFilter() {
		return dirtyContainingAssociationFilter;
	}

	abstract static class AbstractBuilder<E> implements PojoTypeExtendedMappingCollector {
		private final PojoRawTypeIdentifier<E> typeIdentifier;
		private final PersistentClass persistentClass;
		private final String jpaEntityName;
		private final String hibernateOrmEntityName;
		private DocumentIdSourceProperty<?> documentIdSourceProperty;
		private PojoPathFilter dirtyFilter;
		private PojoPathFilter dirtyContainingAssociationFilter;
		private final List<PojoRawTypeIdentifier<? super E>> ascendingSuperTypes;

		AbstractBuilder(PojoRawTypeModel<E> typeModel, PersistentClass persistentClass) {
			this.typeIdentifier = typeModel.typeIdentifier();
			this.persistentClass = persistentClass;
			this.jpaEntityName = persistentClass.getJpaEntityName();
			this.hibernateOrmEntityName = persistentClass.getEntityName();
			this.ascendingSuperTypes = typeModel.ascendingSuperTypes()
					.map( PojoRawTypeModel::typeIdentifier )
					.collect( Collectors.toList() );
		}

		@Override
		public void documentIdSourceProperty(PojoPropertyModel<?> documentIdSourceProperty) {
			this.documentIdSourceProperty = new DocumentIdSourceProperty<>( documentIdSourceProperty );
		}

		@Override
		public void dirtyFilter(PojoPathFilter dirtyFilter) {
			this.dirtyFilter = dirtyFilter;
		}

		@Override
		public void dirtyContainingAssociationFilter(PojoPathFilter filter) {
			this.dirtyContainingAssociationFilter = filter;
		}
	}

}
