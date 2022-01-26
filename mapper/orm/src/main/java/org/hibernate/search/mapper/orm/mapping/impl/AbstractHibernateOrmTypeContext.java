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
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.mapper.orm.event.impl.HibernateOrmListenerTypeContext;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmEntityIdEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmNonEntityIdPropertyEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.impl.LoadingTypeContext;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContext;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeContext;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

abstract class AbstractHibernateOrmTypeContext<E>
		implements PojoTypeContext<E>, HibernateOrmListenerTypeContext, HibernateOrmSessionTypeContext<E>,
		LoadingTypeContext<E> {

	private final PojoRawTypeIdentifier<E> typeIdentifier;
	private final String jpaEntityName;
	private final EntityPersister entityPersister;
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
		this.entityPersister = metamodel.getEntityDescriptor( builder.hibernateOrmEntityName );
		this.ascendingSuperTypes = builder.ascendingSuperTypes;
		if ( builder.documentIdSourcePropertyName != null ) {
			if ( builder.documentIdSourcePropertyName.equals( entityPersister().getIdentifierPropertyName() ) ) {
				documentIdIsEntityId = true;
				loadingStrategy = (HibernateOrmEntityLoadingStrategy<? super E, ?>) HibernateOrmEntityIdEntityLoadingStrategy
						.create( sessionFactory, entityPersister() );
			}
			else {
				// The entity ID is not the property used to generate the document ID
				// We need to use a criteria query to load entities from the document IDs
				documentIdIsEntityId = false;
				loadingStrategy = (HibernateOrmEntityLoadingStrategy<? super E,
						?>) HibernateOrmNonEntityIdPropertyEntityLoadingStrategy.create( sessionFactory, entityPersister(),
								builder.documentIdSourcePropertyName, builder.documentIdSourcePropertyHandle
						);
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
		return entityPersister.getEntityName();
	}

	@Override
	public EntityPersister entityPersister() {
		return entityPersister;
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
		private final String jpaEntityName;
		private final String hibernateOrmEntityName;
		private String documentIdSourcePropertyName;
		private ValueReadHandle<?> documentIdSourcePropertyHandle;
		private PojoPathFilter dirtyFilter;
		private PojoPathFilter dirtyContainingAssociationFilter;
		private final List<PojoRawTypeIdentifier<? super E>> ascendingSuperTypes;

		AbstractBuilder(PojoRawTypeModel<E> typeModel, String jpaEntityName, String hibernateOrmEntityName) {
			this.typeIdentifier = typeModel.typeIdentifier();
			this.jpaEntityName = jpaEntityName;
			this.hibernateOrmEntityName = hibernateOrmEntityName;
			this.ascendingSuperTypes = typeModel.ascendingSuperTypes()
					.map( PojoRawTypeModel::typeIdentifier )
					.collect( Collectors.toList() );
		}

		@Override
		public void documentIdSourceProperty(PojoPropertyModel<?> documentIdSourceProperty) {
			this.documentIdSourcePropertyName = documentIdSourceProperty.name();
			this.documentIdSourcePropertyHandle = documentIdSourceProperty.handle();
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
