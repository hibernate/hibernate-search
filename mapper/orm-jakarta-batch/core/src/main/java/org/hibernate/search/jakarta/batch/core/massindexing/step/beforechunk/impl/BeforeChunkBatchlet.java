/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jakarta.batch.core.massindexing.step.beforechunk.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Objects;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.runtime.context.JobContext;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.UnsupportedOperationBehavior;
import org.hibernate.search.jakarta.batch.core.logging.impl.Log;
import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJobParameters;
import org.hibernate.search.jakarta.batch.core.massindexing.impl.JobContextData;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.SerializationUtil;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.spi.BatchMappingContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;
import org.hibernate.search.util.common.impl.Futures;
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
		boolean purgeAllOnStart = SerializationUtil.parseBooleanParameterOptional(
				MassIndexingJobParameters.PURGE_ALL_ON_START, serializedPurgeAllOnStart,
				MassIndexingJobParameters.Defaults.PURGE_ALL_ON_START
		);
		boolean dropAndCreateSchemaOnStart = SerializationUtil.parseBooleanParameterOptional(
				MassIndexingJobParameters.DROP_AND_CREATE_SCHEMA_ON_START,
				serializedDropAndCreateSchemaOnStart,
				MassIndexingJobParameters.Defaults.DROP_AND_CREATE_SCHEMA_ON_START
		);
		boolean mergeSegmentsAfterPurge = SerializationUtil.parseBooleanParameterOptional(
				MassIndexingJobParameters.MERGE_SEGMENTS_AFTER_PURGE, serializedMergeSegmentsAfterPurge,
				MassIndexingJobParameters.Defaults.MERGE_SEGMENTS_AFTER_PURGE
		);

		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		EntityManagerFactory emf = jobData.getEntityManagerFactory();
		SearchMapping mapping = Search.mapping( emf );
		BatchMappingContext mappingContext = (BatchMappingContext) mapping;

		if ( serializedPurgeAllOnStart == null && serializedDropAndCreateSchemaOnStart == null ) {
			PojoScopeWorkspace workspace =
					mappingContext.scope( jobData.getEntityTypes() ).pojoWorkspace( Objects.toString( tenantId, null ) );
			Futures.unwrappedExceptionJoin( workspace.purgeOrDrop( OperationSubmitter.blocking(),
					UnsupportedOperationBehavior.FAIL, mergeSegmentsAfterPurge ) );
		}
		else {
			if ( dropAndCreateSchemaOnStart ) {
				if ( tenantId != null ) {
					// If we have a tenant id here, there most likely are other tenants,
					//   and if we drop-create the schema then we'd lose the indexed docs for other tenants.
					//   let the user decide what they want to do here, and either remove the tenant filter,
					//   or do the schema drop through a schema manager.
					throw log.tenantIdProvidedWithSchemaDrop( tenantId );
				}
				mapping.scope( jobData.getEntityTypes() ).schemaManager().dropAndCreate();
			}

			if ( Boolean.TRUE.equals( dropAndCreateSchemaOnStart )
					&& ( serializedPurgeAllOnStart != null && Boolean.TRUE.equals( purgeAllOnStart ) ) ) {
				log.redundantPurgeAfterDrop();
			}

			// No need to purge if we've dropped-created the schema already
			if ( purgeAllOnStart && !dropAndCreateSchemaOnStart ) {
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
		}

		return null;
	}
}
