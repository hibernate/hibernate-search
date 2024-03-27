/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.impl;

import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.mapper.pojo.loading.definition.spi.PojoEntityLoadingBindingContext;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.PojoModelElement;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBinder;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBindingContext;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoMassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoSelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.work.impl.SearchIndexingPlanTypeContext;

abstract class AbstractStandalonePojoTypeContext<E>
		implements SearchIndexingPlanTypeContext<E> {

	private final PojoRawTypeIdentifier<E> typeIdentifier;
	private final String entityName;
	private final PojoPathFilter dirtyFilter;

	AbstractStandalonePojoTypeContext(AbstractBuilder<E> builder) {
		this.typeIdentifier = builder.typeIdentifier;
		this.entityName = builder.entityName;
		this.dirtyFilter = builder.dirtyFilter;
	}

	@Override
	public String toString() {
		return typeIdentifier().toString();
	}

	@Override
	public PojoRawTypeIdentifier<E> typeIdentifier() {
		return typeIdentifier;
	}

	public String name() {
		return entityName;
	}

	public Class<E> javaClass() {
		return typeIdentifier.javaClass();
	}

	@Override
	public PojoPathFilter dirtyFilter() {
		return dirtyFilter;
	}

	abstract static class AbstractBuilder<E> implements PojoTypeExtendedMappingCollector {
		private final PojoRawTypeIdentifier<E> typeIdentifier;
		private final String entityName;
		private PojoPathFilter dirtyFilter;

		AbstractBuilder(PojoRawTypeIdentifier<E> typeIdentifier, String entityName) {
			this.typeIdentifier = typeIdentifier;
			this.entityName = entityName;
		}

		@Override
		public void dirtyFilter(PojoPathFilter dirtyFilter) {
			this.dirtyFilter = dirtyFilter;
		}

		@Override
		public void applyLoadingBinder(Object binder, PojoEntityLoadingBindingContext context) {
			var castConfigurer = (EntityLoadingBinder) binder;
			castConfigurer.bind( new EntityLoadingBindingContext() {
				@Override
				public PojoModelElement entityType() {
					return context.entityType();
				}

				@Override
				public PojoModelElement identifierType() {
					return context.identifierType();
				}

				@Override
				public <E2> void selectionLoadingStrategy(Class<E2> expectedEntitySuperType,
						SelectionLoadingStrategy<? super E2> strategy) {
					context.selectionLoadingStrategy( expectedEntitySuperType, strategy == null
							? null
							: new StandalonePojoSelectionLoadingStrategy<>( strategy ) );
				}

				@Override
				public <E2> void massLoadingStrategy(Class<E2> expectedEntitySuperType,
						MassLoadingStrategy<? super E2, ?> strategy) {
					context.massLoadingStrategy( expectedEntitySuperType, strategy == null
							? null
							: new StandalonePojoMassLoadingStrategy<>( strategy ) );
				}

				@Override
				public BeanResolver beanResolver() {
					return context.beanResolver();
				}

				@Override
				public <T> T param(String name, Class<T> paramType) {
					return context.param( name, paramType );
				}

				@Override
				public <T> Optional<T> paramOptional(String name, Class<T> paramType) {
					return context.paramOptional( name, paramType );
				}
			} );
		}
	}
}
