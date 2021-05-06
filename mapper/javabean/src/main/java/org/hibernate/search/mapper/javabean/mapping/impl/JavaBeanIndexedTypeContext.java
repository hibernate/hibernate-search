/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.mapper.javabean.mapping.metadata.impl.JavaBeanEntityTypeMetadata;
import org.hibernate.search.mapper.javabean.scope.impl.JavaBeanScopeIndexedTypeContext;
import org.hibernate.search.mapper.javabean.session.impl.JavaBeanSessionIndexedTypeContext;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexedTypeExtendedMappingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

class JavaBeanIndexedTypeContext<E> extends AbstractJavaBeanTypeContext<E>
		implements JavaBeanScopeIndexedTypeContext<E>, JavaBeanSessionIndexedTypeContext<E> {

	private final MappedIndexManager indexManager;

	private JavaBeanIndexedTypeContext(Builder<E> builder) {
		super( builder );
		this.indexManager = builder.indexManager;
	}

	@Override
	public IndexManager indexManager() {
		return indexManager.toAPI();
	}

	static class Builder<E> extends AbstractBuilder<E> implements PojoIndexedTypeExtendedMappingCollector {
		private MappedIndexManager indexManager;

		Builder(PojoRawTypeIdentifier<E> typeIdentifier, String entityName, JavaBeanEntityTypeMetadata<E> metadata) {
			super( typeIdentifier, entityName, metadata );
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
