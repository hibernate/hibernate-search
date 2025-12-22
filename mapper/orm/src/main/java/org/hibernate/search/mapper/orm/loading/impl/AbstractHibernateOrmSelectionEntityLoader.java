/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.List;

import jakarta.persistence.PersistenceException;

import org.hibernate.QueryTimeoutException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.Query;
import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.mapper.orm.loading.spi.EntityGraphHint;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmLoadingSessionContext;
import org.hibernate.search.mapper.orm.loading.spi.MutableEntityLoadingOptions;
import org.hibernate.search.mapper.orm.search.query.spi.HibernateOrmSearchQueryHints;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionEntityLoader;

abstract class AbstractHibernateOrmSelectionEntityLoader<E> implements PojoSelectionEntityLoader<E> {
	protected static final String IDS_PARAMETER_NAME = "ids";

	protected final EntityMappingType entityMappingType;
	protected final HibernateOrmLoadingSessionContext sessionContext;
	protected final MutableEntityLoadingOptions loadingOptions;
	protected final TypeQueryFactory<E, ?> queryFactory;

	public AbstractHibernateOrmSelectionEntityLoader(EntityMappingType entityMappingType, TypeQueryFactory<E, ?> queryFactory,
			HibernateOrmLoadingSessionContext sessionContext, MutableEntityLoadingOptions loadingOptions) {
		this.entityMappingType = entityMappingType;
		this.sessionContext = sessionContext;
		this.loadingOptions = loadingOptions;
		this.queryFactory = queryFactory;
	}

	@Override
	public final List<E> loadBlocking(List<?> identifiers, Deadline deadline) {
		Long timeout = deadline == null ? null : deadline.checkRemainingTimeMillis();
		try {
			return doLoadEntities( identifiers, timeout );
		}
		catch (QueryTimeoutException | jakarta.persistence.QueryTimeoutException | LockTimeoutException |
				jakarta.persistence.LockTimeoutException e) {
			if ( deadline == null ) {
				// ORM-initiated timeout: just propagate the exception.
				throw e;
			}
			throw deadline.forceTimeoutAndCreateException( e );
		}
		// We might get a jakarta.persistence.PersistenceException instead
		//  of a more specific jakarta.persistence.QueryTimeoutException
		//  if tx was marked for rollback, so we need to handle that differently here
		//
		//  See also: https://github.com/hibernate/hibernate-orm/commit/01b3ee1f9f2949390e0cb2d9065915f379da5a1f
		catch (PersistenceException pe) {
			if ( pe.getCause() instanceof QueryTimeoutException ) {
				if ( deadline == null ) {
					// ORM-initiated timeout: just propagate the exception.
					throw pe;
				}
				throw deadline.forceTimeoutAndCreateException( pe );
			}
			throw pe;
		}
	}

	abstract List<E> doLoadEntities(List<?> allIds, Long timeout);

	@SuppressWarnings("unchecked")
	final Query<E> createQuery(int fetchSize, Long timeout) {
		Query<E> query = queryFactory.createQueryForLoadByUniqueProperty( sessionContext.session(), IDS_PARAMETER_NAME );

		query.setFetchSize( fetchSize );
		if ( timeout != null ) {
			query.setHint( HibernateOrmSearchQueryHints.JAVAX_TIMEOUT, Math.toIntExact( timeout ) );
		}

		// TODO: HSEARCH-5318 use different API for the entity graphs:
		EntityGraphHint<E> entityGraphHint =
				(EntityGraphHint<E>) loadingOptions.entityGraphHintOrNullForType( entityMappingType );
		if ( entityGraphHint != null ) {
			query.setEntityGraph( entityGraphHint.graph, entityGraphHint.semantic );
		}

		return query;
	}
}
