/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.step.beforechunk.impl;

import java.util.Collections;
import java.util.Objects;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.runtime.context.JobContext;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.UnsupportedOperationBehavior;
import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJobParameters;
import org.hibernate.search.jakarta.batch.core.massindexing.impl.JobContextData;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.SerializationUtil;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.spi.BatchMappingContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;
import org.hibernate.search.util.common.impl.Futures;

/**
 * Enhancements before the chunk step {@code produceLuceneDoc} (lucene document production)
 *
 * @author Mincong Huang
 */
public class BeforeChunkBatchlet extends AbstractBatchlet {

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
				MassIndexingJobParameters.PURGE_ALL_ON_START, serializedPurgeAllOnStart,
				MassIndexingJobParameters.Defaults.PURGE_ALL_ON_START
		);
		boolean mergeSegmentsAfterPurge = SerializationUtil.parseBooleanParameterOptional(
				MassIndexingJobParameters.MERGE_SEGMENTS_AFTER_PURGE, serializedMergeSegmentsAfterPurge,
				MassIndexingJobParameters.Defaults.MERGE_SEGMENTS_AFTER_PURGE
		);

		if ( purgeAllOnStart ) {
			JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
			EntityManagerFactory emf = jobData.getEntityManagerFactory();
			BatchMappingContext mappingContext = (BatchMappingContext) Search.mapping( emf );
			// TODO : .........
			PojoScopeWorkspace workspace =
					mappingContext.scope( Object.class ).pojoWorkspace( Objects.toString( tenantId, null ) );
			Futures.unwrappedExceptionJoin( workspace.purge( Collections.emptySet(), OperationSubmitter.blocking(),
					UnsupportedOperationBehavior.FAIL ) );

			// This does not look necessary as (in Hibernate Search 6+) the purge should already commit
			// its changes, even on Lucene.
			// TODO HSEARCH-4487 remove this while we're refactoring?
			Futures.unwrappedExceptionJoin( workspace.flush( OperationSubmitter.blocking(),
					// If not supported, we're on Amazon OpenSearch Serverless,
					// and in this case purge writes are safe even without a flush.
					UnsupportedOperationBehavior.IGNORE ) );

			if ( mergeSegmentsAfterPurge ) {
				Futures.unwrappedExceptionJoin( workspace.mergeSegments( OperationSubmitter.blocking(),
						serializedMergeSegmentsAfterPurge != null
								? UnsupportedOperationBehavior.FAIL
								: UnsupportedOperationBehavior.IGNORE ) );
			}
		}
		return null;
	}
}
