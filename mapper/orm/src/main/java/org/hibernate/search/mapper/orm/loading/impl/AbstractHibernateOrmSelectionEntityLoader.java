/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.List;

import org.hibernate.QueryTimeoutException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.jpa.QueryHints;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;
import org.hibernate.search.engine.common.timing.spi.Deadline;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionEntityLoader;

abstract class AbstractHibernateOrmSelectionEntityLoader<E> implements PojoSelectionEntityLoader<E> {
	protected static final String IDS_PARAMETER_NAME = "ids";

	protected final EntityPersister entityPersister;
	protected final LoadingSessionContext sessionContext;
	protected final MutableEntityLoadingOptions loadingOptions;
	protected final boolean singleConcreteTypeInHierarchy;
	protected final TypeQueryFactory<E, ?> queryFactory;

	public AbstractHibernateOrmSelectionEntityLoader(EntityPersister entityPersister, TypeQueryFactory<E, ?> queryFactory,
			LoadingSessionContext sessionContext, MutableEntityLoadingOptions loadingOptions) {
		this.entityPersister = entityPersister;
		this.sessionContext = sessionContext;
		this.loadingOptions = loadingOptions;
		this.singleConcreteTypeInHierarchy = HibernateOrmUtils.hasAtMostOneConcreteSubType(
				sessionContext.session().getSessionFactory(), entityPersister );
		this.queryFactory = queryFactory;
	}

	@Override
	public final List<?> loadBlocking(List<?> identifiers, Deadline deadline) {
		Long timeout = deadline == null ? null : deadline.remainingTimeMillis();
		try {
			return doLoadEntities( identifiers, timeout );
		}
		catch (QueryTimeoutException | javax.persistence.QueryTimeoutException | LockTimeoutException |
				javax.persistence.LockTimeoutException e) {
			if ( deadline == null ) {
				// ORM-initiated timeout: just propagate the exception.
				throw e;
			}
			throw deadline.forceTimeoutAndCreateException( e );
		}
	}

	abstract List<?> doLoadEntities(List<?> allIds, Long timeout);

	final Query<E> createQuery(int fetchSize, Long timeout) {
		Query<E> query = queryFactory.createQueryForLoadByUniqueProperty( sessionContext.session(), IDS_PARAMETER_NAME );

		query.setFetchSize( fetchSize );
		if ( timeout != null ) {
			query.setHint( QueryHints.SPEC_HINT_TIMEOUT, Math.toIntExact( timeout ) );
		}

		EntityGraphHint<?> entityGraphHint = loadingOptions.entityGraphHintOrNullForType( entityPersister );
		if ( entityGraphHint != null ) {
			query.applyGraph( entityGraphHint.graph, entityGraphHint.semantic );
		}

		return query;
	}
}
