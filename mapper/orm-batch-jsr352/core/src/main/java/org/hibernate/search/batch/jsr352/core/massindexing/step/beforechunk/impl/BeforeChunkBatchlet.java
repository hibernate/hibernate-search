/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.massindexing.step.beforechunk.impl;

import static org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters.MERGE_SEGMENTS_AFTER_PURGE;
import static org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters.PURGE_ALL_ON_START;

import java.lang.invoke.MethodHandles;
import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.runtime.context.JobContext;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.batch.jsr352.core.logging.impl.Log;
import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters;
import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters.Defaults;
import org.hibernate.search.batch.jsr352.core.massindexing.impl.JobContextData;
import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.SerializationUtil;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.work.SearchWorkspace;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

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
	@BatchProperty(name = MassIndexingJobParameters.MERGE_SEGMENTS_AFTER_PURGE)
	private String serializedMergeSegmentsAfterPurge;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.TENANT_ID)
	private String tenantId;

	@Override
	public String process() throws Exception {
		boolean purgeAllOnStart = SerializationUtil.parseBooleanParameterOptional(
				PURGE_ALL_ON_START, serializedPurgeAllOnStart, Defaults.PURGE_ALL_ON_START
		);
		boolean mergeSegmentsAfterPurge = SerializationUtil.parseBooleanParameterOptional(
				MERGE_SEGMENTS_AFTER_PURGE, serializedMergeSegmentsAfterPurge, Defaults.MERGE_SEGMENTS_AFTER_PURGE
		);

		if ( purgeAllOnStart ) {
			JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
			EntityManagerFactory emf = jobData.getEntityManagerFactory();
			SearchWorkspace workspace = Search.mapping( emf ).scope( Object.class ).workspace( tenantId );
			workspace.purge();

			// This is necessary because the batchlet is not executed inside a transaction
			workspace.flush();

			if ( mergeSegmentsAfterPurge ) {
				log.startMergeSegments();
				workspace.mergeSegments();
			}
		}
		return null;
	}
}
