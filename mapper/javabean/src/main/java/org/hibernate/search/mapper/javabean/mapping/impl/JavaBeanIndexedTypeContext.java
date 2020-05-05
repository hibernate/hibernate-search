/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.mapper.javabean.session.impl.JavaBeanSessionIndexedTypeContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.IdentifierMapping;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

class JavaBeanIndexedTypeContext<E> implements JavaBeanSessionIndexedTypeContext<E> {
	private final PojoRawTypeIdentifier<E> typeIdentifier;
	private final String entityName;
	private IdentifierMapping identifierMapping;

	private JavaBeanIndexedTypeContext(Builder<E> builder) {
		this.typeIdentifier = builder.typeIdentifier;
		this.entityName = builder.entityName;
		this.identifierMapping = builder.identifierMapping;
	}

	@Override
	public PojoRawTypeIdentifier<E> getTypeIdentifier() {
		return typeIdentifier;
	}

	@Override
	public String getEntityName() {
		return entityName;
	}

	@Override
	public IdentifierMapping getIdentifierMapping() {
		return identifierMapping;
	}

	static class Builder<E> implements PojoIndexedTypeExtendedMappingCollector {
		private final PojoRawTypeIdentifier<E> typeIdentifier;
		private final String entityName;
		private IdentifierMapping identifierMapping;

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
			// Nothing to do
		}

		JavaBeanIndexedTypeContext<E> build() {
			return new JavaBeanIndexedTypeContext<>( this );
		}
	}
}
