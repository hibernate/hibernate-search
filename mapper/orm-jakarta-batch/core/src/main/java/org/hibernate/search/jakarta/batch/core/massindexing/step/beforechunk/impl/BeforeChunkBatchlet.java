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
import org.hibernate.search.jakarta.batch.core.logging.impl.JakartaBatchLog;
import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJobParameters;
import org.hibernate.search.jakarta.batch.core.massindexing.impl.JobContextData;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.SerializationUtil;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.spi.BatchMappingContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingDefaultCleanOperation;
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
	@BatchProperty(name = MassIndexingJobParameters.DROP_AND_CREATE_SCHEMA_ON_START)
	private String serializedDropAndCreateSchemaOnStart;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.MERGE_SEGMENTS_AFTER_PURGE)
	private String serializedMergeSegmentsAfterPurge;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.TENANT_ID)
	private String tenantId;

	@Override
	public String process() throws Exception {
		final JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		final boolean purgeDefault;
		final boolean dropDefault;

		if ( serializedDropAndCreateSchemaOnStart == null && serializedPurgeAllOnStart == null ) {
			// no parameters for cleaning the data were provided hence we should decide on the defaults:
			MassIndexingDefaultCleanOperation operation = jobData.getMassIndexingDefaultCleanOperation();
			purgeDefault = MassIndexingDefaultCleanOperation.PURGE.equals( operation );
			dropDefault = MassIndexingDefaultCleanOperation.DROP_AND_CREATE.equals( operation );
		}
		else {
			purgeDefault = MassIndexingJobParameters.Defaults.PURGE_ALL_ON_START;
			dropDefault = MassIndexingJobParameters.Defaults.DROP_AND_CREATE_SCHEMA_ON_START;
		}

		boolean purgeAllOnStart = SerializationUtil.parseBooleanParameterOptional(
				MassIndexingJobParameters.PURGE_ALL_ON_START,
				serializedPurgeAllOnStart,
				purgeDefault
		);
		boolean dropAndCreateSchemaOnStart = SerializationUtil.parseBooleanParameterOptional(
				MassIndexingJobParameters.DROP_AND_CREATE_SCHEMA_ON_START,
				serializedDropAndCreateSchemaOnStart,
				dropDefault
		);
		boolean mergeSegmentsAfterPurge = SerializationUtil.parseBooleanParameterOptional(
				MassIndexingJobParameters.MERGE_SEGMENTS_AFTER_PURGE, serializedMergeSegmentsAfterPurge,
				MassIndexingJobParameters.Defaults.MERGE_SEGMENTS_AFTER_PURGE
		);

		if ( dropAndCreateSchemaOnStart ) {
			if ( tenantId != null ) {
				// If we have a tenant id here, there most likely are other tenants,
				//   and if we drop-create the schema then we'd lose the indexed docs for other tenants.
				//   let the user decide what they want to do here, and either remove the tenant filter,
				//   or do the schema drop through a schema manager.
				throw JakartaBatchLog.INSTANCE.tenantIdProvidedWithSchemaDrop( tenantId );
			}
			EntityManagerFactory emf = jobData.getEntityManagerFactory();
			Search.mapping( emf ).scope( jobData.getEntityTypes() ).schemaManager().dropAndCreate();
		}

		if ( Boolean.TRUE.equals( dropAndCreateSchemaOnStart )
				&& ( serializedPurgeAllOnStart != null && Boolean.TRUE.equals( purgeAllOnStart ) ) ) {
			JakartaBatchLog.INSTANCE.redundantPurgeAfterDrop();
		}

		// No need to purge if we've dropped-created the schema already
		if ( purgeAllOnStart && !dropAndCreateSchemaOnStart ) {
			EntityManagerFactory emf = jobData.getEntityManagerFactory();
			BatchMappingContext mappingContext = (BatchMappingContext) Search.mapping( emf );
			PojoScopeWorkspace workspace =
					mappingContext.scope( jobData.getEntityTypes() ).pojoWorkspace( Objects.toString( tenantId, null ) );
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
