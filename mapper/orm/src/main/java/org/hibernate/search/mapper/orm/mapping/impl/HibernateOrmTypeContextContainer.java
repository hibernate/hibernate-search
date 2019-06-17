/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeTypeContextProvider;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

class HibernateOrmTypeContextContainer implements HibernateOrmScopeTypeContextProvider {

	private final Map<Class<?>, HibernateOrmIndexedTypeContext<?>> indexedTypeContexts;
	private final Map<Class<?>, HibernateOrmContainedTypeContext<?>> containedTypeContexts;

	private HibernateOrmTypeContextContainer(Builder builder) {
		this.indexedTypeContexts = builder.buildIndexedTypeContexts();
		this.containedTypeContexts = builder.buildContainedTypeContexts();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> HibernateOrmIndexedTypeContext<E> getIndexedByExactClass(Class<E> clazz) {
		return (HibernateOrmIndexedTypeContext<E>) indexedTypeContexts.get( clazz );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> HibernateOrmContainedTypeContext<E> getContainedByExactClass(Class<E> clazz) {
		return (HibernateOrmContainedTypeContext<E>) containedTypeContexts.get( clazz );
	}

	@Override
	public <E> AbstractHibernateOrmTypeContext<E> getByExactClass(Class<E> clazz) {
		AbstractHibernateOrmTypeContext<E> result = getIndexedByExactClass( clazz );
		if ( result != null ) {
			return result;
		}

		result = getContainedByExactClass( clazz );

		return result;
	}

	static class Builder {

		// Use a LinkedHashMap for deterministic iteration
		private final Map<Class<?>, HibernateOrmIndexedTypeContext.Builder<?>> indexedTypeContextBuilders = new LinkedHashMap<>();
		private final Map<Class<?>, HibernateOrmContainedTypeContext.Builder<?>> containedTypeContextBuilders = new LinkedHashMap<>();

		Builder() {
		}

		<E> HibernateOrmIndexedTypeContext.Builder<E> addIndexed(PojoRawTypeModel<E> typeModel) {
			HibernateOrmIndexedTypeContext.Builder<E> builder =
					new HibernateOrmIndexedTypeContext.Builder<>( typeModel.getJavaClass() );
			indexedTypeContextBuilders.put( typeModel.getJavaClass(), builder );
			return builder;
		}

		<E> HibernateOrmContainedTypeContext.Builder<E> addContained(PojoRawTypeModel<E> typeModel) {
			HibernateOrmContainedTypeContext.Builder<E> builder =
					new HibernateOrmContainedTypeContext.Builder<>( typeModel.getJavaClass() );
			containedTypeContextBuilders.put( typeModel.getJavaClass(), builder );
			return builder;
		}

		HibernateOrmTypeContextContainer build() {
			return new HibernateOrmTypeContextContainer( this );
		}

		private Map<Class<?>, HibernateOrmIndexedTypeContext<?>> buildIndexedTypeContexts() {
			Map<Class<?>, HibernateOrmIndexedTypeContext<?>> typeContexts = new LinkedHashMap<>();
			for ( Map.Entry<Class<?>, HibernateOrmIndexedTypeContext.Builder<?>> entry :
					indexedTypeContextBuilders.entrySet() ) {
				typeContexts.put( entry.getKey(), entry.getValue().build() );
			}
			return typeContexts;
		}

		private Map<Class<?>, HibernateOrmContainedTypeContext<?>> buildContainedTypeContexts() {
			Map<Class<?>, HibernateOrmContainedTypeContext<?>> typeContexts = new LinkedHashMap<>();
			for ( Map.Entry<Class<?>, HibernateOrmContainedTypeContext.Builder<?>> entry :
					containedTypeContextBuilders.entrySet() ) {
				typeContexts.put( entry.getKey(), entry.getValue().build() );
			}
			return typeContexts;
		}
	}

}
