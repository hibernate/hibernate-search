/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.impl;

import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.standalone.session.impl.StandalonePojoSessionIndexedTypeContext;

class StandalonePojoIndexedTypeContext<E> extends AbstractStandalonePojoTypeContext<E>
		implements SearchIndexedEntity<E>, StandalonePojoSessionIndexedTypeContext<E> {

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

		Builder(PojoRawTypeIdentifier<E> typeIdentifier, String entityName) {
			super( typeIdentifier, entityName );
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
