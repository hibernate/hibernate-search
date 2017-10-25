/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.steps.afterchunk;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters;
import org.hibernate.search.jsr352.massindexing.impl.JobContextData;
import org.jboss.logging.Logger;

/**
 * Enhancements after the chunk step {@code produceLuceneDoc} (lucene document production)
 *
 * @author Mincong Huang
 */
public class AfterChunkBatchlet extends AbstractBatchlet {

	private static final Logger LOGGER = Logger.getLogger( AfterChunkBatchlet.class );

	@Inject
	private JobContext jobContext;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.OPTIMIZE_ON_FINISH)
	private String optimizeOnFinish;

	private Session session;

	@Override
	public String process() throws Exception {
		if ( Boolean.parseBoolean( this.optimizeOnFinish ) ) {
			LOGGER.info( "optimizing all entities ..." );

			JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
			EntityManagerFactory emf = jobData.getEntityManagerFactory();
			session = emf.unwrap( SessionFactory.class ).openSession();
			ContextHelper.getSearchIntegrator( session ).optimize();
		}
		return null;
	}

	@Override
	public void stop() throws Exception {
		try {
			session.close();
		}
		catch (Exception e) {
			LOGGER.error( e );
		}
	}
}
