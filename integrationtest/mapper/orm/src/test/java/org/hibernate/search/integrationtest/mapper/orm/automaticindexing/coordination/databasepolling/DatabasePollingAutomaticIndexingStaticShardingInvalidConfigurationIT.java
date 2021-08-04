/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.coordination.databasepolling;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.UnaryOperator;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-4141")
public class DatabasePollingAutomaticIndexingStaticShardingInvalidConfigurationIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.automaticIndexingStrategy( CoordinationStrategyExpectations.outboxPolling() );

	@Test
	public void totalCount_missing() {
		assertThatThrownBy( () -> setup( context -> context ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.failure( "Invalid value for configuration property 'hibernate.search.coordination.shards.total_count'",
								"''", "When using static sharding, this property must be set" )
						.build() );
	}

	@Test
	public void totalCount_zero() {
		assertThatThrownBy( () -> setup( context -> context
				.withProperty( "hibernate.search.coordination.shards.total_count", "0" ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.failure( "Invalid value for configuration property 'hibernate.search.coordination.shards.total_count'",
								"'0'", "The total shard count must be strictly positive" )
						.build() );
	}

	@Test
	public void totalCount_negative() {
		assertThatThrownBy( () -> setup( context -> context
				.withProperty( "hibernate.search.coordination.shards.total_count", "-1" ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.failure( "Invalid value for configuration property 'hibernate.search.coordination.shards.total_count'",
								"'-1'", "The total shard count must be strictly positive" )
						.build() );
	}

	@Test
	public void assigned_missing() {
		assertThatThrownBy( () -> setup( context -> context
				.withProperty( "hibernate.search.coordination.shards.total_count", "10" ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.failure( "Invalid value for configuration property 'hibernate.search.coordination.shards.assigned'",
								"''", "When using static sharding, this property must be set" )
						.build() );
	}

	@Test
	public void assigned_negative() {
		assertThatThrownBy( () -> setup( context -> context
				.withProperty( "hibernate.search.coordination.shards.total_count", "10" )
				.withProperty( "hibernate.search.coordination.shards.assigned", "-1" ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.failure( "Invalid value for configuration property 'hibernate.search.coordination.shards.assigned'",
								"'-1'",
								"Shard indices must be between 0 (inclusive) and 10 (exclusive,"
										+ " set by 'hibernate.search.coordination.shards.total_count')" )
						.build() );
	}

	@Test
	public void assigned_equalToTotalCount() {
		assertThatThrownBy( () -> setup( context -> context
				.withProperty( "hibernate.search.coordination.shards.total_count", "10" )
				.withProperty( "hibernate.search.coordination.shards.assigned", "10" ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.failure( "Invalid value for configuration property 'hibernate.search.coordination.shards.assigned'",
								"'10'",
								"Shard indices must be between 0 (inclusive) and 10 (exclusive,"
										+ " set by 'hibernate.search.coordination.shards.total_count')" )
						.build() );
	}

	@Test
	public void assigned_greaterThanTotalCount() {
		assertThatThrownBy( () -> setup( context -> context
				.withProperty( "hibernate.search.coordination.shards.total_count", "10" )
				.withProperty( "hibernate.search.coordination.shards.assigned", "11" ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.failure( "Invalid value for configuration property 'hibernate.search.coordination.shards.assigned'",
								"'11'",
								"Shard indices must be between 0 (inclusive) and 10 (exclusive,"
										+ " set by 'hibernate.search.coordination.shards.total_count')" )
						.build() );
	}

	private void setup(UnaryOperator<OrmSetupHelper.SetupContext> config) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) ) );
		ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.shards.static", "true" )
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
