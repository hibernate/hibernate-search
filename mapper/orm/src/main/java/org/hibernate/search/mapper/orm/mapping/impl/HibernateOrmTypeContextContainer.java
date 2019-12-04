/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmBasicTypeMetadataProvider;
import org.hibernate.search.mapper.orm.event.impl.HibernateOrmListenerTypeContextProvider;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmRawTypeIdentifierResolver;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionIndexedTypeContext;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContextProvider;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class HibernateOrmTypeContextContainer implements HibernateOrmListenerTypeContextProvider, HibernateOrmSessionTypeContextProvider {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// Use a LinkedHashMap for deterministic iteration
	private final Map<PojoRawTypeIdentifier<?>, HibernateOrmIndexedTypeContext<?>> indexedTypeContexts = new LinkedHashMap<>();
	private final Map<String, HibernateOrmIndexedTypeContext<?>> indexedTypeContextsByHibernateOrmEntityName = new LinkedHashMap<>();
	private final Map<String, HibernateOrmIndexedTypeContext<?>> indexedTypeContextsByIndexName = new LinkedHashMap<>();
	private final Map<PojoRawTypeIdentifier<?>, HibernateOrmContainedTypeContext<?>> containedTypeContexts = new LinkedHashMap<>();
	private final Map<String, HibernateOrmContainedTypeContext<?>> containedTypeContextsByHibernateOrmEntityName = new LinkedHashMap<>();

	private final HibernateOrmRawTypeIdentifierResolver typeIdentifierResolver;

	private HibernateOrmTypeContextContainer(Builder builder, SessionFactoryImplementor sessionFactory) {
		for ( HibernateOrmIndexedTypeContext.Builder<?> contextBuilder : builder.indexedTypeContextBuilders ) {
			HibernateOrmIndexedTypeContext<?> indexedTypeContext = contextBuilder.build( sessionFactory );
			PojoRawTypeIdentifier<?> typeIdentifier = indexedTypeContext.getTypeIdentifier();
			indexedTypeContexts.put( typeIdentifier, indexedTypeContext );
			indexedTypeContextsByHibernateOrmEntityName.put( indexedTypeContext.getHibernateOrmEntityName(), indexedTypeContext );
			indexedTypeContextsByIndexName.put( indexedTypeContext.getIndexName(), indexedTypeContext );
		}
		for ( HibernateOrmContainedTypeContext.Builder<?> contextBuilder : builder.containedTypeContextBuilders ) {
			HibernateOrmContainedTypeContext<?> containedTypeContext = contextBuilder.build( sessionFactory );
			PojoRawTypeIdentifier<?> typeIdentifier = containedTypeContext.getTypeIdentifier();
			containedTypeContexts.put( typeIdentifier, containedTypeContext );
			containedTypeContextsByHibernateOrmEntityName.put( containedTypeContext.getHibernateOrmEntityName(), containedTypeContext );
		}

		this.typeIdentifierResolver = builder.basicTypeMetadataProvider.getTypeIdentifierResolver();
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
	public AbstractHibernateOrmTypeContext<?> getByHibernateOrmEntityName(String hibernateOrmEntityName) {
		AbstractHibernateOrmTypeContext<?> result =
				indexedTypeContextsByHibernateOrmEntityName.get( hibernateOrmEntityName );
		if ( result != null ) {
			return result;
		}

		result = containedTypeContextsByHibernateOrmEntityName.get( hibernateOrmEntityName );

		return result;
	}

	@Override
	public <T> PojoRawTypeIdentifier<T> getTypeIdentifierByJavaClass(Class<T> clazz) {
		return typeIdentifierResolver.resolveByJavaClass( clazz );
	}

	@Override
	public PojoRawTypeIdentifier<?> getTypeIdentifierByHibernateOrmEntityName(String entityName) {
		PojoRawTypeIdentifier<?> result = typeIdentifierResolver.resolveByHibernateOrmEntityName( entityName );
		if ( result == null ) {
			throw log.invalidEntityName( entityName, typeIdentifierResolver.getKnownHibernateOrmEntityNames() );
		}
		return result;
	}

	static class Builder {

		private final HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider;
		private final List<HibernateOrmIndexedTypeContext.Builder<?>> indexedTypeContextBuilders = new ArrayList<>();
		private final List<HibernateOrmContainedTypeContext.Builder<?>> containedTypeContextBuilders = new ArrayList<>();

		Builder(HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider) {
			this.basicTypeMetadataProvider = basicTypeMetadataProvider;
		}

		<E> HibernateOrmIndexedTypeContext.Builder<E> addIndexed(PojoRawTypeModel<E> typeModel, String jpaEntityName,
				String indexName) {
			HibernateOrmIndexedTypeContext.Builder<E> builder = new HibernateOrmIndexedTypeContext.Builder<>(
					typeModel.getTypeIdentifier(),
					jpaEntityName, basicTypeMetadataProvider.getHibernateOrmEntityNameByJpaEntityName( jpaEntityName ),
					indexName
			);
			indexedTypeContextBuilders.add( builder );
			return builder;
		}

		<E> HibernateOrmContainedTypeContext.Builder<E> addContained(PojoRawTypeModel<E> typeModel, String jpaEntityName) {
			HibernateOrmContainedTypeContext.Builder<E> builder = new HibernateOrmContainedTypeContext.Builder<>(
					typeModel.getTypeIdentifier(),
					jpaEntityName, basicTypeMetadataProvider.getHibernateOrmEntityNameByJpaEntityName( jpaEntityName )
			);
			containedTypeContextBuilders.add( builder );
			return builder;
		}

		HibernateOrmTypeContextContainer build(SessionFactoryImplementor sessionFactory) {
			return new HibernateOrmTypeContextContainer( this, sessionFactory );
		}
	}

}
