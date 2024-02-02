/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.impl;

import org.hibernate.search.mapper.pojo.loading.definition.spi.PojoEntityLoadingBindingContext;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoMassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoSelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.mapping.metadata.EntityConfigurationContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.metadata.EntityConfigurer;
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
			@SuppressWarnings("unchecked") // We make sure of that in APIs, see org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurationContext.addEntityType(java.lang.Class<E>, org.hibernate.search.mapper.pojo.standalone.mapping.metadata.EntityConfigurer<E>)
			var castConfigurer = (EntityConfigurer<E>) binder;
			castConfigurer.configure( new EntityConfigurationContext<>() {
				@Override
				public void selectionLoadingStrategy(SelectionLoadingStrategy<? super E> strategy) {
					context.selectionLoadingStrategy( typeIdentifier.javaClass(), strategy == null
							? null
							: new StandalonePojoSelectionLoadingStrategy<>( strategy ) );
				}

				@Override
				public void massLoadingStrategy(MassLoadingStrategy<? super E, ?> strategy) {
					context.massLoadingStrategy( typeIdentifier.javaClass(), strategy == null
							? null
							: new StandalonePojoMassLoadingStrategy<>( strategy ) );
				}
			} );
		}
	}
}
