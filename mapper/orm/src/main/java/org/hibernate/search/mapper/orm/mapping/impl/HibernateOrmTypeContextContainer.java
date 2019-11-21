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
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

class HibernateOrmTypeContextContainer implements HibernateOrmSessionTypeContextProvider {

	// Use a LinkedHashMap for deterministic iteration
	private final Map<PojoRawTypeIdentifier<?>, HibernateOrmIndexedTypeContext<?>> indexedTypeContexts = new LinkedHashMap<>();
	private final Map<String, HibernateOrmIndexedTypeContext<?>> indexedTypeContextsByHibernateOrmEntityName = new LinkedHashMap<>();
	private final Map<String, HibernateOrmIndexedTypeContext<?>> indexedTypeContextsByIndexName = new LinkedHashMap<>();
	private final Map<PojoRawTypeIdentifier<?>, HibernateOrmContainedTypeContext<?>> containedTypeContexts = new LinkedHashMap<>();
	private final Map<String, HibernateOrmContainedTypeContext<?>> containedTypeContextsByHibernateOrmEntityName = new LinkedHashMap<>();

	private HibernateOrmTypeContextContainer(Builder builder, SessionFactoryImplementor sessionFactory) {
		for ( HibernateOrmIndexedTypeContext.Builder<?> contextBuilder : builder.indexedTypeContextBuilders ) {
			HibernateOrmIndexedTypeContext<?> indexedTypeContext = contextBuilder.build( sessionFactory );
			indexedTypeContexts.put( indexedTypeContext.getTypeIdentifier(), indexedTypeContext );
			indexedTypeContextsByHibernateOrmEntityName.put( indexedTypeContext.getEntityType().getTypeName(), indexedTypeContext );
			indexedTypeContextsByIndexName.put( indexedTypeContext.getIndexName(), indexedTypeContext );
		}
		for ( HibernateOrmContainedTypeContext.Builder<?> contextBuilder : builder.containedTypeContextBuilders ) {
			HibernateOrmContainedTypeContext<?> containedTypeContext = contextBuilder.build( sessionFactory );
			containedTypeContexts.put( containedTypeContext.getTypeIdentifier(), containedTypeContext );
			containedTypeContextsByHibernateOrmEntityName.put( containedTypeContext.getEntityType().getTypeName(), containedTypeContext );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> HibernateOrmIndexedTypeContext<E> getIndexedByExactType(PojoRawTypeIdentifier<E> typeIdentifier) {
		return (HibernateOrmIndexedTypeContext<E>) indexedTypeContexts.get( typeIdentifier );
	}

	@Override
	public HibernateOrmSessionIndexedTypeContext getByIndexName(String indexName) {
		return indexedTypeContextsByIndexName.get( indexName );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> HibernateOrmContainedTypeContext<E> getContainedByExactType(PojoRawTypeIdentifier<E> typeIdentifier) {
		return (HibernateOrmContainedTypeContext<E>) containedTypeContexts.get( typeIdentifier );
	}

	@Override
	public <E> AbstractHibernateOrmTypeContext<E> getByExactType(PojoRawTypeIdentifier<E> typeIdentifier) {
		AbstractHibernateOrmTypeContext<E> result = getIndexedByExactType( typeIdentifier );
		if ( result != null ) {
			return result;
		}

		result = getContainedByExactType( typeIdentifier );

		return result;
	}

	@Override
	public AbstractHibernateOrmTypeContext<?> getByHibernateOrmEntityName(String entityName) {
		AbstractHibernateOrmTypeContext<?> result = indexedTypeContextsByHibernateOrmEntityName.get( entityName );
		if ( result != null ) {
			return result;
		}

		result = containedTypeContextsByHibernateOrmEntityName.get( entityName );

		return result;
	}

	static class Builder {

		private final List<HibernateOrmIndexedTypeContext.Builder<?>> indexedTypeContextBuilders = new ArrayList<>();
		private final List<HibernateOrmContainedTypeContext.Builder<?>> containedTypeContextBuilders = new ArrayList<>();

		Builder() {
		}

		<E> HibernateOrmIndexedTypeContext.Builder<E> addIndexed(PojoRawTypeModel<E> typeModel, String entityName,
				String indexName) {
			HibernateOrmIndexedTypeContext.Builder<E> builder =
					new HibernateOrmIndexedTypeContext.Builder<>( typeModel.getTypeIdentifier(), entityName, indexName );
			indexedTypeContextBuilders.add( builder );
			return builder;
		}

		<E> HibernateOrmContainedTypeContext.Builder<E> addContained(PojoRawTypeModel<E> typeModel, String entityName) {
			HibernateOrmContainedTypeContext.Builder<E> builder =
					new HibernateOrmContainedTypeContext.Builder<>( typeModel.getTypeIdentifier(), entityName );
			containedTypeContextBuilders.add( builder );
			return builder;
		}

		HibernateOrmTypeContextContainer build(SessionFactoryImplementor sessionFactory) {
			return new HibernateOrmTypeContextContainer( this, sessionFactory );
		}
	}

}
