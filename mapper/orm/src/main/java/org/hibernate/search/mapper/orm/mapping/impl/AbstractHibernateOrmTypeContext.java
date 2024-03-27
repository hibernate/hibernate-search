/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.search.mapper.orm.event.impl.HibernateOrmListenerTypeContext;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmLoadingTypeContext;
import org.hibernate.search.mapper.orm.model.impl.DocumentIdSourceProperty;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContext;
import org.hibernate.search.mapper.pojo.loading.definition.spi.PojoEntityLoadingBindingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeContext;

abstract class AbstractHibernateOrmTypeContext<E>
		implements PojoTypeContext<E>, HibernateOrmListenerTypeContext, HibernateOrmSessionTypeContext<E>,
		HibernateOrmLoadingTypeContext<E> {

	private final PojoRawTypeIdentifier<E> typeIdentifier;
	private final PojoLoadingTypeContext<E> delegate;
	private final String jpaEntityName;
	private final EntityMappingType entityMappingType;
	private final boolean documentIdIsEntityId;
	private final HibernateOrmEntityLoadingStrategy<? super E, ?> loadingStrategy;
	private final PojoPathFilter dirtyFilter;
	private final PojoPathFilter dirtyContainingAssociationFilter;

	AbstractHibernateOrmTypeContext(Builder<E> builder, PojoLoadingTypeContext<E> delegate,
			SessionFactoryImplementor sessionFactory) {
		this.typeIdentifier = builder.typeIdentifier;
		this.delegate = delegate;
		this.jpaEntityName = builder.jpaEntityName;
		MappingMetamodel metamodel = sessionFactory.getMappingMetamodel();
		this.entityMappingType = metamodel.getEntityDescriptor( builder.hibernateOrmEntityName );
		this.documentIdIsEntityId = builder.documentIdSourceProperty != null
				&& builder.documentIdSourceProperty.name.equals( entityMappingType.getIdentifierMapping().getAttributeName() );
		this.loadingStrategy = builder.loadingStrategy;
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
	public PojoLoadingTypeContext<E> delegate() {
		return delegate;
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

	public abstract static class Builder<E> implements PojoTypeExtendedMappingCollector {
		public final PojoRawTypeIdentifier<E> typeIdentifier;
		private final PersistentClass persistentClass;
		private final String jpaEntityName;
		private final String hibernateOrmEntityName;
		private DocumentIdSourceProperty<?> documentIdSourceProperty;
		private PojoPathFilter dirtyFilter;
		private PojoPathFilter dirtyContainingAssociationFilter;
		private HibernateOrmEntityLoadingStrategy<? super E, ?> loadingStrategy;

		Builder(PojoRawTypeModel<E> typeModel, PersistentClass persistentClass) {
			this.typeIdentifier = typeModel.typeIdentifier();
			this.persistentClass = persistentClass;
			this.jpaEntityName = persistentClass.getJpaEntityName();
			this.hibernateOrmEntityName = persistentClass.getEntityName();
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

		@Override
		@SuppressWarnings("unchecked") // The binder uses reflection to create a strategy of the appropriate type
		public void applyLoadingBinder(Object binder, PojoEntityLoadingBindingContext context) {
			var castBinder = (HibernateOrmEntityLoadingBinder) binder;
			this.loadingStrategy = (HibernateOrmEntityLoadingStrategy<? super E, ?>) castBinder
					.createLoadingStrategy( persistentClass, documentIdSourceProperty );
			if ( this.loadingStrategy != null ) {
				context.selectionLoadingStrategy( typeIdentifier.javaClass(), this.loadingStrategy );
				context.massLoadingStrategy( typeIdentifier.javaClass(), this.loadingStrategy );
			}
		}
	}

}
