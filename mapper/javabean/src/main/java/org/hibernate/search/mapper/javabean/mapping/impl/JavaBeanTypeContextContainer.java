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
	private final Map<PojoRawTypeIdentifier<?>, AbstractJavaBeanTypeContext<?>> typeContexts = new LinkedHashMap<>();

	private JavaBeanTypeContextContainer(Builder builder) {
		for ( JavaBeanIndexedTypeContext.Builder<?> contextBuilder : builder.indexedTypeContextBuilders ) {
			JavaBeanIndexedTypeContext<?> typeContext = contextBuilder.build();
			indexedTypeContexts.put( typeContext.typeIdentifier(), typeContext );
			indexedTypeContextsByClass.put( typeContext.javaClass(), typeContext );
			indexedTypeContextsByEntityName.put( typeContext.name(), typeContext );
			typeContexts.put( typeContext.typeIdentifier(), typeContext );
		}
		for ( JavaBeanContainedTypeContext.Builder<?> contextBuilder : builder.containedTypeContextBuilders ) {
			JavaBeanContainedTypeContext<?> typeContext = contextBuilder.build();
			typeContexts.put( typeContext.typeIdentifier(), typeContext );
		}
	}

	@SuppressWarnings("unchecked")
	public <E> JavaBeanIndexedTypeContext<E> indexedForExactType(PojoRawTypeIdentifier<E> typeIdentifier) {
		return (JavaBeanIndexedTypeContext<E>) indexedTypeContexts.get( typeIdentifier );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> AbstractJavaBeanTypeContext<E> forExactType(PojoRawTypeIdentifier<E> typeIdentifier) {
		return (AbstractJavaBeanTypeContext<E>) typeContexts.get( typeIdentifier );
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
		private final List<JavaBeanContainedTypeContext.Builder<?>> containedTypeContextBuilders = new ArrayList<>();

		Builder() {
		}

		<E> JavaBeanIndexedTypeContext.Builder<E> addIndexed(PojoRawTypeModel<E> typeModel, String entityName) {
			JavaBeanIndexedTypeContext.Builder<E> builder =
					new JavaBeanIndexedTypeContext.Builder<>( typeModel.typeIdentifier(), entityName );
			indexedTypeContextBuilders.add( builder );
			return builder;
		}

		<E> JavaBeanContainedTypeContext.Builder<E> addContained(PojoRawTypeModel<E> typeModel, String entityName) {
			JavaBeanContainedTypeContext.Builder<E> builder =
					new JavaBeanContainedTypeContext.Builder<>( typeModel.typeIdentifier(), entityName );
			containedTypeContextBuilders.add( builder );
			return builder;
		}

		JavaBeanTypeContextContainer build() {
			return new JavaBeanTypeContextContainer( this );
		}
	}

}
