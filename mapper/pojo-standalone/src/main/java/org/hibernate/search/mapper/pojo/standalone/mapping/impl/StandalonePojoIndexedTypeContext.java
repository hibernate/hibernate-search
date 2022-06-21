/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.impl;

import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.mapper.pojo.standalone.mapping.metadata.impl.StandalonePojoEntityTypeMetadata;
import org.hibernate.search.mapper.pojo.standalone.scope.impl.StandalonePojoScopeIndexedTypeContext;
import org.hibernate.search.mapper.pojo.standalone.session.impl.StandalonePojoSessionIndexedTypeContext;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

class StandalonePojoIndexedTypeContext<E> extends AbstractStandalonePojoTypeContext<E>
		implements StandalonePojoScopeIndexedTypeContext<E>, StandalonePojoSessionIndexedTypeContext<E> {

	private final MappedIndexManager indexManager;

	private StandalonePojoIndexedTypeContext(Builder<E> builder) {
		super( builder );
		this.indexManager = builder.indexManager;
	}

	@Override
	public IndexManager indexManager() {
		return indexManager.toAPI();
	}

	static class Builder<E> extends AbstractBuilder<E> implements PojoIndexedTypeExtendedMappingCollector {
		private MappedIndexManager indexManager;

		Builder(PojoRawTypeIdentifier<E> typeIdentifier, String entityName, StandalonePojoEntityTypeMetadata<E> metadata) {
			super( typeIdentifier, entityName, metadata );
		}

		@Override
		public void indexManager(MappedIndexManager indexManager) {
			this.indexManager = indexManager;
		}

		StandalonePojoIndexedTypeContext<E> build() {
			return new StandalonePojoIndexedTypeContext<>( this );
		}
	}
}
