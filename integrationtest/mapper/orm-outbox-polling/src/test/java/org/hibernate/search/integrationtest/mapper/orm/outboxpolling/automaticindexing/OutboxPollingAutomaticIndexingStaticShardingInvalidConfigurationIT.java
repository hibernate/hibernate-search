/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.UnaryOperator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-4141")
class OutboxPollingAutomaticIndexingStaticShardingInvalidConfigurationIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper =
			OrmSetupHelper.withCoordinationStrategy( CoordinationStrategyExpectations.outboxPolling() )
					.withBackendMock( backendMock );

	@Test
	void totalCount_missing() {
		assertThatThrownBy( () -> setup( context -> context
				.withProperty( "hibernate.search.coordination.event_processor.shards.assigned", "0" ) ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.failure(
								"Invalid value for configuration property 'hibernate.search.coordination.event_processor.shards.total_count'",
								"''",
								"This property must be set when 'hibernate.search.coordination.event_processor.shards.assigned' is set" ) );
	}

	@Test
	void totalCount_zero() {
		assertThatThrownBy( () -> setup( context -> context
				.withProperty( "hibernate.search.coordination.event_processor.shards.total_count", "0" ) ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.failure(
								"Invalid value for configuration property 'hibernate.search.coordination.event_processor.shards.total_count'",
								"'0'", "'value' must be strictly positive" ) );
	}

	@Test
	void totalCount_negative() {
		assertThatThrownBy( () -> setup( context -> context
				.withProperty( "hibernate.search.coordination.event_processor.shards.total_count", "-1" ) ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.failure(
								"Invalid value for configuration property 'hibernate.search.coordination.event_processor.shards.total_count'",
								"'-1'", "'value' must be strictly positive" ) );
	}

	@Test
	void assigned_missing() {
		assertThatThrownBy( () -> setup( context -> context
				.withProperty( "hibernate.search.coordination.event_processor.shards.total_count", "10" ) ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.failure(
								"Invalid value for configuration property 'hibernate.search.coordination.event_processor.shards.assigned'",
								"''",
								"This property must be set when 'hibernate.search.coordination.event_processor.shards.total_count' is set" ) );
	}

	@Test
	void assigned_negative() {
		assertThatThrownBy( () -> setup( context -> context
				.withProperty( "hibernate.search.coordination.event_processor.shards.total_count", "10" )
				.withProperty( "hibernate.search.coordination.event_processor.shards.assigned", "-1" ) ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.failure(
								"Invalid value for configuration property 'hibernate.search.coordination.event_processor.shards.assigned'",
								"'-1'",
								"'value' must be positive or zero" ) );
	}

	@Test
	void assigned_equalToTotalCount() {
		assertThatThrownBy( () -> setup( context -> context
				.withProperty( "hibernate.search.coordination.event_processor.shards.total_count", "10" )
				.withProperty( "hibernate.search.coordination.event_processor.shards.assigned", "10" ) ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.failure(
								"Invalid value for configuration property 'hibernate.search.coordination.event_processor.shards.assigned'",
								"'10'",
								"Shard indices must be between 0 (inclusive) and 10 (exclusive,"
										+ " set by 'hibernate.search.coordination.event_processor.shards.total_count')" ) );
	}

	@Test
	void assigned_greaterThanTotalCount() {
		assertThatThrownBy( () -> setup( context -> context
				.withProperty( "hibernate.search.coordination.event_processor.shards.total_count", "10" )
				.withProperty( "hibernate.search.coordination.event_processor.shards.assigned", "11" ) ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.failure(
								"Invalid value for configuration property 'hibernate.search.coordination.event_processor.shards.assigned'",
								"'11'",
								"Shard indices must be between 0 (inclusive) and 10 (exclusive,"
										+ " set by 'hibernate.search.coordination.event_processor.shards.total_count')" ) );
	}

	private void setup(UnaryOperator<OrmSetupHelper.SetupContext> config) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) ) );
		ormSetupHelper.start()
				.with( config )
				.setup( IndexedEntity.class );
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static class IndexedEntity {

		static final String NAME = "IndexedEntity";

		@Id
		private Integer id;
		@FullTextField
		private String text;

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

}
