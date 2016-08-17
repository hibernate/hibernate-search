/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.afterChunk;

import java.util.Set;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.backend.spi.BatchBackend;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.jsr352.internal.JobContextData;
import org.jboss.logging.Logger;

/**
 * Enhancements after the chunk step "produceLuceneDoc" (lucene document
 * production)
 *
 * @author Mincong Huang
 */
@Named
public class AfterChunkBatchlet extends AbstractBatchlet {

	private static final Logger LOGGER = Logger.getLogger( AfterChunkBatchlet.class );
	private final JobContext jobContext;

	@Inject
	@BatchProperty
	private boolean optimizeAtEnd;

	@PersistenceUnit(unitName = "h2")
	private EntityManagerFactory emf;

	private Session session;

	@Inject
	public AfterChunkBatchlet(JobContext jobContext) {
		this.jobContext = jobContext;
	}

	@Override
	public String process() throws Exception {

		if ( this.optimizeAtEnd ) {

			LOGGER.info( "purging index for all entities ..." );
			session = emf.unwrap( SessionFactory.class ).openSession();
			final BatchBackend backend = ContextHelper
					.getSearchintegrator( session )
					.makeBatchBackend( null );

			LOGGER.info( "optimizing all entities ..." );
			JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
			Set<Class<?>> targetedClasses = jobData.getEntityClazzSet();
			backend.optimize( targetedClasses );
			backend.flush( targetedClasses );
		}
		return null;
	}

	@Override
	public void stop() throws Exception {
		try {
			session.close();
		}
		catch ( Exception e ) {
			LOGGER.error( e );
		}
	}
}
