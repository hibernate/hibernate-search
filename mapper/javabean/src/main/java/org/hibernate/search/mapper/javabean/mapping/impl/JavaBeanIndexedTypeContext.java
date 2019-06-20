/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import org.hibernate.search.mapper.javabean.session.impl.JavaBeanSessionIndexedTypeContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.spi.IdentifierMapping;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector;

class JavaBeanIndexedTypeContext<E> implements JavaBeanSessionIndexedTypeContext<E> {
	private final Class<E> javaClass;
	private final String indexName;
	private IdentifierMapping identifierMapping;

	private JavaBeanIndexedTypeContext(Builder<E> builder) {
		this.javaClass = builder.javaClass;
		this.indexName = builder.indexName;
		this.identifierMapping = builder.identifierMapping;
	}

	@Override
	public Class<E> getJavaClass() {
		return javaClass;
	}

	String getIndexName() {
		return indexName;
	}

	@Override
	public IdentifierMapping getIdentifierMapping() {
		return identifierMapping;
	}

	static class Builder<E> implements PojoIndexedTypeExtendedMappingCollector {
		private final Class<E> javaClass;
		private final String indexName;
		private IdentifierMapping identifierMapping;

		Builder(Class<E> javaClass, String indexName) {
			this.javaClass = javaClass;
			this.indexName = indexName;
		}

		@Override
		public void documentIdSourcePropertyName(String documentIdSourcePropertyName) {
			// Nothing to do
		}

		@Override
		public void identifierMapping(IdentifierMapping identifierMapping) {
			this.identifierMapping = identifierMapping;
		}

		JavaBeanIndexedTypeContext<E> build() {
			return new JavaBeanIndexedTypeContext<>( this );
		}
	}
}
