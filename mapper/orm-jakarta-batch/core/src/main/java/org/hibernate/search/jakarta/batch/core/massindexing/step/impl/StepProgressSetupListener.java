/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.step.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.listener.AbstractStepListener;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.StatelessSession;
import org.hibernate.search.jakarta.batch.core.logging.impl.JakartaBatchLog;
import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJobParameters;
import org.hibernate.search.jakarta.batch.core.massindexing.impl.JobContextData;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.BatchCoreEntityTypeDescriptor;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.PersistenceUtil;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.SerializationUtil;
import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchIdentifierLoader;
import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchIdentifierLoadingOptions;
import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchReindexCondition;

/**
 * Listener for managing the step indexing progress.
 *
 * @author Mincong Huang
 */
public class StepProgressSetupListener extends AbstractStepListener {

	@Inject
	private JobContext jobContext;

	@Inject
	private StepContext stepContext;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.TENANT_ID)
	private String tenantId;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.REINDEX_ONLY_HQL)
	private String reindexOnlyHql;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.REINDEX_ONLY_PARAMETERS)
	private String serializedReindexOnlyParameters;

	/**
	 * Setup the step-level indexing progress. The {@code StepProgress} will be initialized if this is the first start,
	 * or remain as it is if this is a restart. {@code StepProgress} is stored as the transient user data <b>for the
	 * principle thread</b>(*). Transient user data is shared with {@code ProgressAggregator}, therefore,
	 * {@code ProgressAggregator} can update the indexing progress through transient user data.
	 * <p>
	 * (*): for partitions' sub-threads, they store other things as a transient user data.
	 */
	@Override
	public void beforeStep() throws IOException, ClassNotFoundException {
		StepProgress stepProgress = (StepProgress) stepContext.getPersistentUserData();

		if ( stepProgress == null ) {
			stepProgress = new StepProgress();
			JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
			EntityManagerFactory emf = jobData.getEntityManagerFactory();
			HibernateOrmBatchReindexCondition reindexOnly =
					SerializationUtil.parseReindexOnlyParameters( reindexOnlyHql, serializedReindexOnlyParameters );

			try ( StatelessSession session =
					PersistenceUtil.openStatelessSession( emf, jobData.getTenancyConfiguration().convert( tenantId ) ) ) {
				ReindexConditionLoadingOptions options = new ReindexConditionLoadingOptions( reindexOnly, session );
				for ( BatchCoreEntityTypeDescriptor<?, ?> type : jobData.getEntityTypeDescriptors() ) {
					try ( var loader = createIdentifierLoader( type, options ) ) {
						OptionalLong count = loader.totalCount();
						if ( count.isPresent() ) {
							JakartaBatchLog.INSTANCE.rowsToIndex( type.jpaEntityName(), count.getAsLong() );
							stepProgress.setRowsToIndex( type.jpaEntityName(), count.getAsLong() );
						}
					}
				}
			}
		}
		stepContext.setTransientUserData( stepProgress );
	}

	private <E> HibernateOrmBatchIdentifierLoader createIdentifierLoader(BatchCoreEntityTypeDescriptor<E, ?> type,
			ReindexConditionLoadingOptions options) {
		return type.batchLoadingStrategy().createIdentifierLoader( type, options );
	}

	/**
	 * Persist the step-level indexing progress after the end of the step's execution. This method is called when the
	 * step is terminated by any reason, e.g. finished, stopped.
	 */
	@Override
	public void afterStep() {
		StepProgress stepProgress = (StepProgress) stepContext.getTransientUserData();
		stepContext.setPersistentUserData( stepProgress );
	}

	private static class ReindexConditionLoadingOptions implements HibernateOrmBatchIdentifierLoadingOptions {

		private final HibernateOrmBatchReindexCondition reindexOnly;
		private final Map<Class<?>, Object> contextData;

		ReindexConditionLoadingOptions(HibernateOrmBatchReindexCondition reindexOnly, StatelessSession session) {
			this.reindexOnly = reindexOnly;
			this.contextData = new HashMap<>();

			this.contextData.put( StatelessSession.class, session );
		}

		@Override
		public int fetchSize() {
			return 1;
		}

		@Override
		public OptionalInt maxResults() {
			return OptionalInt.empty();
		}

		@Override
		public OptionalInt offset() {
			return OptionalInt.empty();
		}

		@Override
		public Optional<HibernateOrmBatchReindexCondition> reindexOnlyCondition() {
			return Optional.ofNullable( reindexOnly );
		}

		@Override
		public Optional<Object> upperBound() {
			return Optional.empty();
		}

		@Override
		public boolean upperBoundInclusive() {
			return false;
		}

		@Override
		public Optional<Object> lowerBound() {
			return Optional.empty();
		}

		@Override
		public boolean lowerBoundInclusive() {
			return false;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T context(Class<T> contextType) {
			return (T) contextData.get( contextType );
		}
	}

}
