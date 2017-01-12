/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import javax.batch.api.listener.AbstractStepListener;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.search.jsr352.internal.JobContextData;
import org.jboss.logging.Logger;

/**
 * Listener for managing the step indexing progress.
 *
 * @author Mincong Huang
 */
@Named
public class StepProgressSetupListener extends AbstractStepListener {

	private static final Logger LOGGER = Logger.getLogger( StepProgressSetupListener.class );
	private final JobContext jobContext;
	private final StepContext stepContext;

	@Inject
	public StepProgressSetupListener(JobContext jobContext, StepContext stepContext) {
		this.jobContext = jobContext;
		this.stepContext = stepContext;
	}

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
		LOGGER.infof( "%d rows to index for entity type %s", rowCount, clazz.getName() );
		return rowCount;
	}
}
