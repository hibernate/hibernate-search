/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.loading.impl;

import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.LockModeType;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;
import org.hibernate.query.Query;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.IdOrder;
import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchIdentifierLoader;
import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchIdentifierLoadingOptions;
import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchLoadingTypeContext;
import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchReindexCondition;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;

public class BatchCoreDefaultHibernateOrmBatchIdentifierLoader<E> implements HibernateOrmBatchIdentifierLoader {

	private final StatelessSession session;
	private final String ormEntityName;
	private final String uniquePropertyName;
	private final IdOrder idOrder;
	private final HibernateOrmBatchIdentifierLoadingOptions options;
	private final IdLoader idLoader;

	public BatchCoreDefaultHibernateOrmBatchIdentifierLoader(HibernateOrmBatchLoadingTypeContext<E> typeContext,
			HibernateOrmBatchIdentifierLoadingOptions options, IdOrder idOrder) {
		this.session = options.context( StatelessSession.class );
		this.ormEntityName = typeContext.jpaEntityName();
		this.uniquePropertyName = typeContext.uniquePropertyName();
		this.idOrder = idOrder;
		this.options = options;
		this.idLoader = options.maxResults().orElse( -1 ) == 1 ? new QuerySingleIdLoader() : new ScrollIdLoader();
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			if ( idLoader != null ) {
				closer.push( IdLoader::close, idLoader );
			}
		}
	}

	@Override
	public OptionalLong totalCount() {
		StringBuilder query = new StringBuilder();
		query.append( "select count(e) from " )
				.append( ormEntityName )
				.append( " e " );

		return OptionalLong.of( createQuery( session, query,
				options.reindexOnlyCondition().map( Set::of ).orElseGet( Set::of ), Long.class, Optional.empty() )
				.uniqueResult() );
	}

	@Override
	public Object next() {
		return idLoader.next();
	}

	@Override
	public boolean hasNext() {
		return idLoader.hasNext();
	}

	private Query<Object> createQueryLoading(StatelessSession session) {
		StringBuilder query = new StringBuilder();
		query.append( "select e." )
				.append( uniquePropertyName )
				.append( " from " )
				.append( ormEntityName )
				.append( " e " );
		Set<HibernateOrmBatchReindexCondition> conditions = new HashSet<>();
		options.reindexOnlyCondition().ifPresent( conditions::add );
		options.lowerBound().ifPresent( b -> conditions
				.add( idOrder.idGreater( "HIBERNATE_SEARCH_ID_LOWER_BOUND_", b, options.lowerBoundInclusive() ) ) );
		options.upperBound().ifPresent( b -> conditions
				.add( idOrder.idLesser( "HIBERNATE_SEARCH_ID_UPPER_BOUND_", b, options.upperBoundInclusive() ) ) );

		Query<Object> select = createQuery( session, query, conditions, Object.class, Optional.of( idOrder.ascOrder() ) )
				.setFetchSize( options.fetchSize() )
				.setReadOnly( true )
				.setCacheable( false )
				.setLockMode( LockModeType.NONE );
		options.offset().ifPresent( select::setFirstResult );
		options.maxResults().ifPresent( select::setMaxResults );
		return select;
	}

	private <T> Query<T> createQuery(StatelessSession session,
			StringBuilder hql, Set<HibernateOrmBatchReindexCondition> conditions, Class<T> returnedType,
			Optional<String> order) {
		if ( !conditions.isEmpty() ) {
			hql.append( " where " );
			hql.append( conditions.stream()
					.map( c -> "( " + c.conditionString() + " )" )
					.collect( Collectors.joining( " AND ", " ", " " ) )
			);
		}
		order.ifPresent( o -> hql.append( " ORDER BY " ).append( o ) );
		Query<T> query = session.createQuery( hql.toString(), returnedType )
				.setCacheable( false );

		for ( var condition : conditions ) {
			for ( var entry : condition.params().entrySet() ) {
				query.setParameter( entry.getKey(), entry.getValue() );
			}
		}

		return query;
	}

	private interface IdLoader {
		Object next();

		boolean hasNext();

		void close();
	}

	private class QuerySingleIdLoader implements IdLoader {

		private boolean hasNextCalled = false;
		private boolean nextCalled = false;

		private Query<Object> id = createQueryLoading( session );
		private Object currentId;

		@Override
		public Object next() {
			if ( hasNextCalled ) {
				nextCalled = true;
				hasNextCalled = false;
				return currentId;
			}
			else {
				throw new AssertionFailure( "Cannot call next() before calling hasNext()" );
			}
		}

		@Override
		public boolean hasNext() {
			if ( nextCalled ) {
				// we expect to have just a single ID, so if we called next and got the id we don't need to execute the query anymore:
				return false;
			}
			currentId = id.getSingleResultOrNull();
			hasNextCalled = true;
			return currentId != null;
		}

		@Override
		public void close() {
			id = null;
		}
	}

	private class ScrollIdLoader implements IdLoader {
		private ScrollableResults<Object> id = createQueryLoading( session ).scroll( ScrollMode.FORWARD_ONLY );

		@Override
		public Object next() {
			return id.get();
		}

		@Override
		public boolean hasNext() {
			return id.next();
		}

		@Override
		public void close() {
			try ( Closer<RuntimeException> closer = new Closer<>() ) {
				if ( id != null ) {
					closer.push( ScrollableResults::close, id );
					id = null;
				}
			}
		}
	}

}
