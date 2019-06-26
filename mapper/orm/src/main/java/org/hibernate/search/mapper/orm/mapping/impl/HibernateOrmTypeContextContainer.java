/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionIndexedTypeContext;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContextProvider;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

class HibernateOrmTypeContextContainer implements HibernateOrmSessionTypeContextProvider {

	// Use a LinkedHashMap for deterministic iteration
	private final Map<Class<?>, HibernateOrmIndexedTypeContext<?>> indexedTypeContexts = new LinkedHashMap<>();
	private final Map<String, HibernateOrmIndexedTypeContext<?>> indexedTypeContextsByIndexName = new LinkedHashMap<>();
	private final Map<Class<?>, HibernateOrmContainedTypeContext<?>> containedTypeContexts = new LinkedHashMap<>();

	private HibernateOrmTypeContextContainer(Builder builder, SessionFactoryImplementor sessionFactory) {
		for ( HibernateOrmIndexedTypeContext.Builder<?> contextBuilder : builder.indexedTypeContextBuilders ) {
			HibernateOrmIndexedTypeContext<?> indexedTypeContext = contextBuilder.build( sessionFactory );
			indexedTypeContexts.put( indexedTypeContext.getJavaClass(), indexedTypeContext );
			indexedTypeContextsByIndexName.put( indexedTypeContext.getIndexName(), indexedTypeContext );
		}
		for ( HibernateOrmContainedTypeContext.Builder<?> contextBuilder : builder.containedTypeContextBuilders ) {
			HibernateOrmContainedTypeContext<?> containedTypeContext = contextBuilder.build();
			containedTypeContexts.put( containedTypeContext.getJavaClass(), containedTypeContext );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> HibernateOrmIndexedTypeContext<E> getIndexedByExactClass(Class<E> clazz) {
		return (HibernateOrmIndexedTypeContext<E>) indexedTypeContexts.get( clazz );
	}

	@Override
	public HibernateOrmSessionIndexedTypeContext getByIndexName(String indexName) {
		return indexedTypeContextsByIndexName.get( indexName );
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

		private final List<HibernateOrmIndexedTypeContext.Builder<?>> indexedTypeContextBuilders = new ArrayList<>();
		private final List<HibernateOrmContainedTypeContext.Builder<?>> containedTypeContextBuilders = new ArrayList<>();

		Builder() {
		}

		<E> HibernateOrmIndexedTypeContext.Builder<E> addIndexed(PojoRawTypeModel<E> typeModel, String indexName) {
			HibernateOrmIndexedTypeContext.Builder<E> builder =
					new HibernateOrmIndexedTypeContext.Builder<>( typeModel.getJavaClass(), indexName );
			indexedTypeContextBuilders.add( builder );
			return builder;
		}

		<E> HibernateOrmContainedTypeContext.Builder<E> addContained(PojoRawTypeModel<E> typeModel) {
			HibernateOrmContainedTypeContext.Builder<E> builder =
					new HibernateOrmContainedTypeContext.Builder<>( typeModel.getJavaClass() );
			containedTypeContextBuilders.add( builder );
			return builder;
		}

		HibernateOrmTypeContextContainer build(SessionFactoryImplementor sessionFactory) {
			return new HibernateOrmTypeContextContainer( this, sessionFactory );
		}
	}

}
