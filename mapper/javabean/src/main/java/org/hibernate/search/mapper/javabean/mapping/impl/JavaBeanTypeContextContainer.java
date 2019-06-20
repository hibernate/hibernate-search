/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.mapper.javabean.session.impl.JavaBeanSearchSessionTypeContextProvider;
import org.hibernate.search.mapper.javabean.session.impl.JavaBeanSessionIndexedTypeContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

class JavaBeanTypeContextContainer implements JavaBeanSearchSessionTypeContextProvider {

	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, JavaBeanIndexedTypeContext<?>> indexedTypeContextsByIndexName = new LinkedHashMap<>();

	private JavaBeanTypeContextContainer(Builder builder) {
		for ( JavaBeanIndexedTypeContext.Builder<?> contextBuilder : builder.indexedTypeContextBuilders ) {
			JavaBeanIndexedTypeContext<?> indexedTypeContext = contextBuilder.build();
			indexedTypeContextsByIndexName.put( indexedTypeContext.getIndexName(), indexedTypeContext );
		}
	}

	@Override
	public JavaBeanSessionIndexedTypeContext getByIndexName(String indexName) {
		return indexedTypeContextsByIndexName.get( indexName );
	}

	static class Builder {

		private final List<JavaBeanIndexedTypeContext.Builder<?>> indexedTypeContextBuilders = new ArrayList<>();

		Builder() {
		}

		<E> JavaBeanIndexedTypeContext.Builder<E> addIndexed(PojoRawTypeModel<E> typeModel, String indexName) {
			JavaBeanIndexedTypeContext.Builder<E> builder =
					new JavaBeanIndexedTypeContext.Builder<>( typeModel.getJavaClass(), indexName );
			indexedTypeContextBuilders.add( builder );
			return builder;
		}

		JavaBeanTypeContextContainer build() {
			return new JavaBeanTypeContextContainer( this );
		}
	}

}
