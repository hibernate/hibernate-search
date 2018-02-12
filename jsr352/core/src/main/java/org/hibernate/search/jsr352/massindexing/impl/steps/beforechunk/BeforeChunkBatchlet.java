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
import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters;
import org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.Defaults;
import org.hibernate.search.jsr352.massindexing.impl.JobContextData;
import org.hibernate.search.jsr352.massindexing.impl.util.PersistenceUtil;
import org.hibernate.search.jsr352.massindexing.impl.util.SerializationUtil;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.OPTIMIZE_AFTER_PURGE;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.PURGE_ALL_ON_START;

/**
 * Enhancements before the chunk step {@code produceLuceneDoc} (lucene document production)
 *
 * @author Mincong Huang
 */
public class BeforeChunkBatchlet extends AbstractBatchlet {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Inject
	private JobContext jobContext;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.PURGE_ALL_ON_START)
	private String serializedPurgeAllOnStart;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.OPTIMIZE_AFTER_PURGE)
	private String serializedOptimizeAfterPurge;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.TENANT_ID)
	private String tenantId;

	@Override
	public String process() throws Exception {
		boolean purgeAllOnStart = SerializationUtil.parseBooleanParameterOptional(
				PURGE_ALL_ON_START, serializedPurgeAllOnStart, Defaults.PURGE_ALL_ON_START
		);
		boolean optimizeAfterPurge = SerializationUtil.parseBooleanParameterOptional(
				OPTIMIZE_AFTER_PURGE, serializedOptimizeAfterPurge, Defaults.OPTIMIZE_AFTER_PURGE
		);

		if ( purgeAllOnStart ) {
			JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
			EntityManagerFactory emf = jobData.getEntityManagerFactory();
			try ( Session session = PersistenceUtil.openSession( emf, tenantId ) ) {
				FullTextSession fts = Search.getFullTextSession( session );
				jobData.getEntityTypes().forEach( clz -> fts.purgeAll( clz ) );

				// This is necessary because the batchlet is not executed inside a transaction
				fts.flushToIndexes();

				if ( optimizeAfterPurge ) {
					log.startOptimization();
					fts.getSearchFactory().optimize();
				}
			}
		}
		return null;
	}
}
