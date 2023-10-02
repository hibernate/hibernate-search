/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.UnaryOperator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
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

@TestForIssue(jiraKey = "HSEARCH-4140")
class OutboxPollingAutomaticIndexingInvalidConfigurationIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper =
			OrmSetupHelper.withCoordinationStrategy( CoordinationStrategyExpectations.outboxPolling() )
					.withBackendMock( backendMock );

	@Test
	void pulseInterval_negative() {
		assertThatThrownBy( () -> setup( context -> context
				.withProperty( "hibernate.search.coordination.event_processor.pulse_interval", "-1" ) ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.failure(
								"Invalid value for configuration property 'hibernate.search.coordination.event_processor.pulse_interval'",
								"'-1'", "'value' must be strictly positive" ) );
	}

	@Test
	void pulseInterval_zero() {
		assertThatThrownBy( () -> setup( context -> context
				.withProperty( "hibernate.search.coordination.event_processor.pulse_interval", "0" ) ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.failure(
								"Invalid value for configuration property 'hibernate.search.coordination.event_processor.pulse_interval'",
								"'0'", "'value' must be strictly positive" ) );
	}

	@Test
	void pulseInterval_lowerThanPollingInterval() {
		assertThatThrownBy( () -> setup( context -> context
				.withProperty( "hibernate.search.coordination.event_processor.pulse_interval", "40" )
				.withProperty( "hibernate.search.coordination.event_processor.polling_interval", "50" ) ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.failure(
								"Invalid value for configuration property 'hibernate.search.coordination.event_processor.pulse_interval'",
								"'40'", "The pulse interval must be greater than or equal to the polling interval",
								"i.e. in this case at least 50" ) );
	}

	@Test
	void pulseExpiration_negative() {
		assertThatThrownBy( () -> setup( context -> context
				.withProperty( "hibernate.search.coordination.event_processor.pulse_expiration", "-1" ) ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.failure(
								"Invalid value for configuration property 'hibernate.search.coordination.event_processor.pulse_expiration'",
								"'-1'", "'value' must be strictly positive" ) );
	}

	@Test
	void pulseExpiration_zero() {
		assertThatThrownBy( () -> setup( context -> context
				.withProperty( "hibernate.search.coordination.event_processor.pulse_expiration", "0" ) ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.failure(
								"Invalid value for configuration property 'hibernate.search.coordination.event_processor.pulse_expiration'",
								"'0'", "'value' must be strictly positive" ) );
	}

	@Test
	void pulseExpiration_lowerThan3TimesPollingInterval() {
		assertThatThrownBy( () -> setup( context -> context
				.withProperty( "hibernate.search.coordination.event_processor.pulse_expiration", "599" )
				.withProperty( "hibernate.search.coordination.event_processor.pulse_interval", "200" ) ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.failure(
								"Invalid value for configuration property 'hibernate.search.coordination.event_processor.pulse_expiration'",
								"'599'",
								"The pulse expiration must be greater than or equal to 3 times the pulse interval",
								"i.e. in this case at least 600" ) );
	}

	private void setup(UnaryOperator<OrmSetupHelper.SetupContext> config) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) ) );
		ormSetupHelper.start()
				.withProperty( AvailableSettings.HBM2DDL_AUTO, "none" ) // Faster startup
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
