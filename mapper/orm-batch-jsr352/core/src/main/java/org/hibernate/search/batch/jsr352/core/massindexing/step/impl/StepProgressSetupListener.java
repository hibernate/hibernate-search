/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.massindexing.step.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.BiFunction;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.listener.AbstractStepListener;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.search.batch.jsr352.core.logging.impl.Log;
import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters;
import org.hibernate.search.batch.jsr352.core.massindexing.impl.JobContextData;
import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.PersistenceUtil;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Listener for managing the step indexing progress.
 *
 * @author Mincong Huang
 */
public class StepProgressSetupListener extends AbstractStepListener {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Inject
	private JobContext jobContext;

	@Inject
	private StepContext stepContext;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.TENANT_ID)
	private String tenantId;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.CUSTOM_QUERY_HQL)
	private String customQueryHql;

	/**
	 * Setup the step-level indexing progress. The {@code StepProgress} will be initialized if this is the first start,
	 * or remain as it is if this is a restart. {@code StepProgress} is stored as the transient user data <b>for the
	 * principle thread</b>(*). Transient user data is shared with {@code ProgressAggregator}, therefore,
	 * {@code ProgressAggregator} can update the indexing progress through transient user data.
	 * <p>
	 * (*): for partitions' sub-threads, they store other things as a transient user data.
	 */
	@Override
	public void beforeStep() {
		StepProgress stepProgress = (StepProgress) stepContext.getPersistentUserData();

		if ( stepProgress == null ) {
			stepProgress = new StepProgress();
			JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
			EntityManagerFactory emf = jobData.getEntityManagerFactory();

			IndexScope indexScope = PersistenceUtil.getIndexScope( customQueryHql );
			BiFunction<Session, Class<?>, Long> rowCountFunction;
			switch ( indexScope ) {
				case HQL:
					// We can't estimate the number of results when using HQL
					rowCountFunction = (session, clazz) -> null;
					break;

				case FULL_ENTITY:
					rowCountFunction = StepProgressSetupListener::countAll;
					break;

				default:
					// This should never happen.
					throw new IllegalStateException( "Unknown value from enum: " + IndexScope.class );
			}

			try ( Session session = PersistenceUtil.openSession( emf, tenantId ) ) {
				for ( Class<?> entityType : jobData.getEntityTypes() ) {
					Long rowCount = rowCountFunction.apply( session, entityType );
					log.rowsToIndex( entityType.getName(), rowCount );
					stepProgress.setRowsToIndex( entityType.getName(), rowCount );
				}
			}
		}
		stepContext.setTransientUserData( stepProgress );
	}

	/**
	 * Persist the step-level indexing progress after the end of the step's execution. This method is called when the
	 * step is terminated by any reason, e.g. finished, stopped.
	 */
	@Override
	public void afterStep() {
		StepProgress stepProgress = (StepProgress) stepContext.getTransientUserData();
		stepContext.setPersistentUserData( stepProgress );
	}

	private static Long countAll(Session session, Class<?> entityType) {
		CriteriaBuilder builder = session.getCriteriaBuilder();
		CriteriaQuery<Long> criteria = builder.createQuery( Long.class );
		criteria.select( builder.count( criteria.from( entityType ) ) );

		Query<Long> query = session.createQuery( criteria );
		query.setCacheable( false )
				.uniqueResult();

		return query.getSingleResult();
	}
}
