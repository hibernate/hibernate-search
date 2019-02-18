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
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters;
import org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.Defaults;
import org.hibernate.search.jsr352.massindexing.impl.JobContextData;
import org.hibernate.search.jsr352.massindexing.impl.util.PersistenceUtil;
import org.hibernate.search.jsr352.massindexing.impl.util.SerializationUtil;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.OPTIMIZE_ON_FINISH;

/**
 * Enhancements after the chunk step {@code produceLuceneDoc} (lucene document production)
 *
 * @author Mincong Huang
 */
public class AfterChunkBatchlet extends AbstractBatchlet {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Inject
	private JobContext jobContext;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.OPTIMIZE_ON_FINISH)
	private String serializedOptimizeOnFinish;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.TENANT_ID)
	private String tenantId;

	private Session session;

	@Override
	public String process() throws Exception {
		boolean optimizeOnFinish = SerializationUtil.parseBooleanParameterOptional(
				OPTIMIZE_ON_FINISH, serializedOptimizeOnFinish, Defaults.OPTIMIZE_ON_FINISH
		);

		if ( optimizeOnFinish ) {
			log.startOptimization();

			JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
			EntityManagerFactory emf = jobData.getEntityManagerFactory();
			session = PersistenceUtil.openSession( emf, tenantId );
			ContextHelper.getSearchIntegrator( session ).optimize();
		}
		return null;
	}

	@Override
	public void stop() throws Exception {
		session.close();
	}
}
