/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.mapper.javabean.scope.impl.JavaBeanScopeIndexedTypeContext;
import org.hibernate.search.mapper.javabean.session.impl.JavaBeanSessionIndexedTypeContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.IdentifierMapping;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

class JavaBeanIndexedTypeContext<E>
		implements JavaBeanScopeIndexedTypeContext<E>, JavaBeanSessionIndexedTypeContext<E> {
	private final PojoRawTypeIdentifier<E> typeIdentifier;
	private final String entityName;
	private final IdentifierMapping identifierMapping;
	private final MappedIndexManager indexManager;

	private JavaBeanIndexedTypeContext(Builder<E> builder) {
		this.typeIdentifier = builder.typeIdentifier;
		this.entityName = builder.entityName;
		this.identifierMapping = builder.identifierMapping;
		this.indexManager = builder.indexManager;
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

	@Override
	public Class<E> javaClass() {
		return typeIdentifier.javaClass();
	}

	@Override
	public IdentifierMapping identifierMapping() {
		return identifierMapping;
	}

	@Override
	public IndexManager indexManager() {
		return indexManager.toAPI();
	}

	static class Builder<E> implements PojoIndexedTypeExtendedMappingCollector {
		private final PojoRawTypeIdentifier<E> typeIdentifier;
		private final String entityName;
		private IdentifierMapping identifierMapping;
		private MappedIndexManager indexManager;

		Builder(PojoRawTypeIdentifier<E> typeIdentifier, String entityName) {
			this.typeIdentifier = typeIdentifier;
			this.entityName = entityName;
		}

		@Override
		public void documentIdSourceProperty(PojoPropertyModel<?> documentIdSourceProperty) {
			// Nothing to do
		}

		@Override
		public void identifierMapping(IdentifierMapping identifierMapping) {
			this.identifierMapping = identifierMapping;
		}

		@Override
		public void indexManager(MappedIndexManager indexManager) {
			this.indexManager = indexManager;
		}

		JavaBeanIndexedTypeContext<E> build() {
			return new JavaBeanIndexedTypeContext<>( this );
		}
	}
}
