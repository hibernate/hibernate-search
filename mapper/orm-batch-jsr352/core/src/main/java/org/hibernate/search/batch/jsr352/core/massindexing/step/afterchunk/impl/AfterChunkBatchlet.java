/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.massindexing.step.afterchunk.impl;

import static org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters.MERGE_SEGMENTS_ON_FINISH;

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
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

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
	@BatchProperty(name = MassIndexingJobParameters.MERGE_SEGMENTS_ON_FINISH)
	private String serializedMergeSegmentsOnFinish;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.TENANT_ID)
	private String tenantId;

	@Override
	public String process() throws Exception {
		boolean mergeSegmentsOnFinish = SerializationUtil.parseBooleanParameterOptional(
				MERGE_SEGMENTS_ON_FINISH, serializedMergeSegmentsOnFinish, Defaults.MERGE_SEGMENTS_ON_FINISH
		);

		if ( mergeSegmentsOnFinish ) {
			log.startMergeSegments();

			JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
			EntityManagerFactory emf = jobData.getEntityManagerFactory();
			Search.mapping( emf ).scope( Object.class ).workspace( tenantId ).mergeSegments();
		}
		return null;
	}
}
