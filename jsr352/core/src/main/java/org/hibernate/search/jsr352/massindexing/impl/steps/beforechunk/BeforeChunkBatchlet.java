/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.steps.beforechunk;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters;
import org.hibernate.search.jsr352.massindexing.impl.JobContextData;
import org.hibernate.search.jsr352.massindexing.impl.util.PersistenceUtil;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Enhancements before the chunk step {@code produceLuceneDoc} (lucene document production)
 *
 * @author Mincong Huang
 */
public class BeforeChunkBatchlet extends AbstractBatchlet {

	private static final Log log = LoggerFactory.make( Log.class );

	@Inject
	private JobContext jobContext;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.PURGE_ALL_ON_START)
	private String purgeAllOnStart;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.OPTIMIZE_AFTER_PURGE)
	private String optimizeAfterPurge;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.TENANT_ID)
	private String tenantId;

	private Session session;
	private FullTextSession fts;

	@Override
	public String process() throws Exception {
		if ( Boolean.parseBoolean( this.purgeAllOnStart ) ) {
			JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
			EntityManagerFactory emf = jobData.getEntityManagerFactory();
			session = PersistenceUtil.openSession( emf, tenantId );
			fts = Search.getFullTextSession( session );
			jobData.getEntityTypes().forEach( clz -> fts.purgeAll( clz ) );

			if ( Boolean.parseBoolean( this.optimizeAfterPurge ) ) {
				log.startOptimization();
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
			log.unableToCloseSession( e );
		}
	}
}
