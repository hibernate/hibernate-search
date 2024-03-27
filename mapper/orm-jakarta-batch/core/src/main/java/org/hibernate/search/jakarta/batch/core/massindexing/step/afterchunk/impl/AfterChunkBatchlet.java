/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.step.afterchunk.impl;

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
 * Enhancements after the chunk step {@code produceLuceneDoc} (lucene document production)
 *
 * @author Mincong Huang
 */
public class AfterChunkBatchlet extends AbstractBatchlet {

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
				MassIndexingJobParameters.MERGE_SEGMENTS_ON_FINISH, serializedMergeSegmentsOnFinish,
				MassIndexingJobParameters.Defaults.MERGE_SEGMENTS_ON_FINISH
		);

		if ( mergeSegmentsOnFinish ) {
			JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
			EntityManagerFactory emf = jobData.getEntityManagerFactory();
			BatchMappingContext mappingContext = (BatchMappingContext) Search.mapping( emf );
			// TODO : .........
			PojoScopeWorkspace workspace =
					mappingContext.scope( Object.class ).pojoWorkspace( Objects.toString( tenantId, null ) );
			Futures.unwrappedExceptionJoin( workspace.mergeSegments( OperationSubmitter.blocking(),
					serializedMergeSegmentsOnFinish != null
							? UnsupportedOperationBehavior.FAIL
							: UnsupportedOperationBehavior.IGNORE ) );
		}
		return null;
	}
}
