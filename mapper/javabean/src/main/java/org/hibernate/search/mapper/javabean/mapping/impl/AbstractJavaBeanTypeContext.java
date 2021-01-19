/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import org.hibernate.search.mapper.javabean.work.impl.SearchIndexingPlanTypeContext;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

abstract class AbstractJavaBeanTypeContext<E>
		implements SearchIndexingPlanTypeContext {

	private final PojoRawTypeIdentifier<E> typeIdentifier;
	private final String entityName;
	private final PojoPathFilter dirtyFilter;

	AbstractJavaBeanTypeContext(AbstractBuilder<E> builder) {
		this.typeIdentifier = builder.typeIdentifier;
		this.entityName = builder.entityName;
		this.dirtyFilter = builder.dirtyFilter;
	}

	@Override
	public String toString() {
		return typeIdentifier().toString();
	}

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
	}
}
