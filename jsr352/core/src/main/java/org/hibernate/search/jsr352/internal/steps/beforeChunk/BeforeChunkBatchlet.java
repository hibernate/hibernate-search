/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.beforeChunk;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.jsr352.internal.JobContextData;
import org.jboss.logging.Logger;

/**
 * Enhancements before the chunk step {@code produceLuceneDoc} (lucene document production)
 *
 * @author Mincong Huang
 */
@Named
public class BeforeChunkBatchlet extends AbstractBatchlet {

	private static final Logger LOGGER = Logger.getLogger( BeforeChunkBatchlet.class );
	private final JobContext jobContext;

	@Inject
	@BatchProperty
	private String purgeAtStart;

	@Inject
	@BatchProperty
	private String optimizeAfterPurge;

	private Session session;
	private FullTextSession fts;

	@Inject
	public BeforeChunkBatchlet(JobContext jobContext) {
		this.jobContext = jobContext;
	}

	@Override
	public String process() throws Exception {

		if ( Boolean.parseBoolean( this.purgeAtStart ) ) {

			JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
			EntityManagerFactory emf = jobData.getEntityManagerFactory();
			session = emf.unwrap( SessionFactory.class ).openSession();
			fts = Search.getFullTextSession( session );
			jobData.getEntityTypes().forEach( clz -> fts.purgeAll( clz ) );

			if ( Boolean.parseBoolean( this.optimizeAfterPurge ) ) {
				LOGGER.info( "optimizing all entities ..." );
				ContextHelper.getSearchIntegrator( session ).optimize();
			}
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
