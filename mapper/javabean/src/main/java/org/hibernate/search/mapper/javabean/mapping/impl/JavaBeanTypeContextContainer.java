/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.mapper.javabean.session.impl.JavaBeanSearchSessionTypeContextProvider;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

class JavaBeanTypeContextContainer implements JavaBeanSearchSessionTypeContextProvider {

	// Use a LinkedHashMap for deterministic iteration
	private final Map<PojoRawTypeIdentifier<?>, JavaBeanIndexedTypeContext<?>> indexedTypeContexts = new LinkedHashMap<>();
	private final Map<Class<?>, JavaBeanIndexedTypeContext<?>> indexedTypeContextsByClass = new LinkedHashMap<>();
	private final Map<String, JavaBeanIndexedTypeContext<?>> indexedTypeContextsByEntityName = new LinkedHashMap<>();

	private JavaBeanTypeContextContainer(Builder builder) {
		for ( JavaBeanIndexedTypeContext.Builder<?> contextBuilder : builder.indexedTypeContextBuilders ) {
			JavaBeanIndexedTypeContext<?> indexedTypeContext = contextBuilder.build();
			indexedTypeContexts.put( indexedTypeContext.typeIdentifier(), indexedTypeContext );
			indexedTypeContextsByClass.put( indexedTypeContext.javaClass(), indexedTypeContext );
			indexedTypeContextsByEntityName.put( indexedTypeContext.name(), indexedTypeContext );
		}
	}

	@SuppressWarnings("unchecked")
	public <E> JavaBeanIndexedTypeContext<E> indexedForExactType(PojoRawTypeIdentifier<E> typeIdentifier) {
		return (JavaBeanIndexedTypeContext<E>) indexedTypeContexts.get( typeIdentifier );
	}

	@SuppressWarnings("unchecked")
	public <E> JavaBeanIndexedTypeContext<E> indexedForExactClass(Class<E> clazz) {
		return (JavaBeanIndexedTypeContext<E>) indexedTypeContextsByClass.get( clazz );
	}

	@Override
	public JavaBeanIndexedTypeContext<?> indexedForEntityName(String indexName) {
		return indexedTypeContextsByEntityName.get( indexName );
	}

	public Collection<JavaBeanIndexedTypeContext<?>> allIndexed() {
		return indexedTypeContexts.values();
	}

	static class Builder {

		private final List<JavaBeanIndexedTypeContext.Builder<?>> indexedTypeContextBuilders = new ArrayList<>();

		Builder() {
		}

		<E> JavaBeanIndexedTypeContext.Builder<E> addIndexed(PojoRawTypeModel<E> typeModel, String entityName) {
			JavaBeanIndexedTypeContext.Builder<E> builder =
					new JavaBeanIndexedTypeContext.Builder<>( typeModel.typeIdentifier(), entityName );
			indexedTypeContextBuilders.add( builder );
			return builder;
		}

		JavaBeanTypeContextContainer build() {
			return new JavaBeanTypeContextContainer( this );
		}
	}

}
