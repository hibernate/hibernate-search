/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.LoadingTypeContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.metadata.impl.StandalonePojoEntityTypeMetadata;
import org.hibernate.search.mapper.pojo.standalone.work.impl.SearchIndexingPlanTypeContext;

abstract class AbstractStandalonePojoTypeContext<E>
		implements SearchIndexingPlanTypeContext<E>, LoadingTypeContext<E> {

	private final PojoRawTypeIdentifier<E> typeIdentifier;
	private final String entityName;
	private final StandalonePojoEntityTypeMetadata<E> metadata;
	private final PojoPathFilter dirtyFilter;

	AbstractStandalonePojoTypeContext(AbstractBuilder<E> builder) {
		this.typeIdentifier = builder.typeIdentifier;
		this.entityName = builder.entityName;
		this.metadata = builder.metadata;
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

	@Override
	public String name() {
		return entityName;
	}

	public Class<E> javaClass() {
		return typeIdentifier.javaClass();
	}

	@Override
	public Optional<SelectionLoadingStrategy<? super E>> selectionLoadingStrategy() {
		return metadata.selectionLoadingStrategy;
	}

	@Override
	public Optional<MassLoadingStrategy<? super E, ?>> massLoadingStrategy() {
		return metadata.massLoadingStrategy;
	}

	@Override
	public PojoPathFilter dirtyFilter() {
		return dirtyFilter;
	}

	abstract static class AbstractBuilder<E> implements PojoTypeExtendedMappingCollector {
		private final PojoRawTypeIdentifier<E> typeIdentifier;
		private final String entityName;
		private final StandalonePojoEntityTypeMetadata<E> metadata;
		private PojoPathFilter dirtyFilter;

		AbstractBuilder(PojoRawTypeIdentifier<E> typeIdentifier, String entityName,
				StandalonePojoEntityTypeMetadata<E> metadata) {
			this.typeIdentifier = typeIdentifier;
			this.entityName = entityName;
			this.metadata = metadata;
		}

		@Override
		public void dirtyFilter(PojoPathFilter dirtyFilter) {
			this.dirtyFilter = dirtyFilter;
		}
	}
}
