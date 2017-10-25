/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.steps.lucene;

import javax.batch.api.listener.AbstractStepListener;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.jsr352.massindexing.impl.JobContextData;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Listener for managing the step indexing progress.
 *
 * @author Mincong Huang
 */
public class StepProgressSetupListener extends AbstractStepListener {

	private static final Log log = LoggerFactory.make( Log.class );

	@Inject
	private JobContext jobContext;

	@Inject
	private StepContext stepContext;

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

			SessionFactory sessionFactory = emf.unwrap( SessionFactory.class );
			Session session = null;

			try {
				session = sessionFactory.openSession();
				for ( Class<?> entityType : jobData.getEntityTypes() ) {
					long rowCount = rowCount( entityType, session );
					stepProgress.setRowsToIndex( entityType.getName(), rowCount );
				}
			}
			finally {
				if ( session != null ) {
					session.close();
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

	private long rowCount(Class<?> clazz, Session session) {
		long rowCount = (long) session.createCriteria( clazz )
				.setProjection( Projections.rowCount() )
				.setCacheable( false )
				.uniqueResult();
		log.rowsToIndex( clazz.getName(), rowCount );
		return rowCount;
	}
}
