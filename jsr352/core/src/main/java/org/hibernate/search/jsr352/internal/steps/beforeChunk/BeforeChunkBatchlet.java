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
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.Session;
import org.hibernate.search.backend.spi.BatchBackend;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.internal.JobContextData;
import org.jboss.logging.Logger;

/**
 * Enhancements before the chunk step "produceLuceneDoc" (lucene document
 * production)
 *
 * @author Mincong Huang
 */
@Named
public class BeforeChunkBatchlet extends AbstractBatchlet {

	private static final Logger LOGGER = Logger.getLogger( BeforeChunkBatchlet.class );
	private final JobContext jobContext;

	@Inject
	@BatchProperty
	private boolean purgeAtStart;

	@Inject
	@BatchProperty
	private boolean optimizeAfterPurge;

	@PersistenceUnit(unitName = "h2")
	private EntityManagerFactory emf;

	private EntityManager em;
	private FullTextEntityManager ftem;

	@Inject
	public BeforeChunkBatchlet(JobContext jobContext) {
		this.jobContext = jobContext;
	}

	@Override
	public String process() throws Exception {

		if ( this.purgeAtStart ) {

			em = emf.createEntityManager();
			ftem = Search.getFullTextEntityManager( em );
			Session session = em.unwrap( Session.class );
			final BatchBackend backend = ContextHelper
					.getSearchintegrator( session )
					.makeBatchBackend( null );
			JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
			jobData.getEntityClazzSet().forEach( clz -> ftem.purgeAll( clz ) );

			if ( this.optimizeAfterPurge ) {
				LOGGER.info( "optimizing all entities ..." );
				// TODO issue #113
				// I don't know what optimize is doing and how to modify it
				backend.optimize( jobData.getEntityClazzSet() );
			}
		}
		return null;
	}

	@Override
	public void stop() throws Exception {
		try {
			em.close();
		}
		catch ( Exception e ) {
			LOGGER.error( e );
		}
	}
}
