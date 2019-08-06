/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.bootstrap;

import java.util.regex.Pattern;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Check that no unexpected logs occur during bootstrap.
 * This won't catch all the problems,
 * but at least we will be aware if a warning or error is logged on every boot.
 */
@TestForIssue(jiraKey = "HSEARCH-3644")
public class BootstrapLogsIT {

	private static final Pattern CONNECTION_POOL_WARNING_PATTERN = Pattern.compile(
			"CachingRegionFactory should be only used for testing"
					+ "||Using Hibernate built-in connection pool"
	);

	private static final Pattern HBM2DDL_WARNING_PATTERN = Pattern.compile(
			"^SQL Warning code"
					+ "||^(relation|table) .* does not exist, skipping"
					+ "||^GenerationTarget encountered exception accepting command:.*"
	);

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	public void noSuspiciousLogEvents() {
		logged.expectEvent( suspiciousLogEventMatcher() ).never(); // Also fails if a higher severity event (e.g. error) is logged

		backendMock.expectAnySchema( IndexedEntity.NAME );

		ormSetupHelper.start()
				.setup( IndexedEntity.class, ContainedEntity.class );
	}

	private static Matcher<? extends LoggingEvent> suspiciousLogEventMatcher() {
		return new TypeSafeMatcher<LoggingEvent>() {
			private final Level level = Level.WARN;
			@Override
			public void describeTo(Description description) {
				description.appendText( "a LoggingEvent with " ).appendValue( level ).appendText( " level or higher" )
						.appendText( " (ignoring known test-only warnings)" );
			}
			@Override
			protected boolean matchesSafely(LoggingEvent item) {
				return item.getLevel().isGreaterOrEqual( level )
						// Ignore these, they are warning but are expected (just related to the testing infrastructure)
						&& !(
								CONNECTION_POOL_WARNING_PATTERN.matcher( item.getRenderedMessage() ).find()
								|| HBM2DDL_WARNING_PATTERN.matcher( item.getRenderedMessage() ).find()
						);
			}
		};
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed(index = IndexedEntity.NAME)
	private static class IndexedEntity {

		static final String NAME = "IndexedEntity";

		@Id
		private Integer id;

		@GenericField
		private String text;

		@OneToOne
		@IndexedEmbedded
		private ContainedEntity contained;
	}

	@Entity(name = ContainedEntity.NAME)
	private static class ContainedEntity {

		static final String NAME = "ContainedEntity";

		@Id
		private Integer id;

		@GenericField
		private String text;

		@OneToOne(mappedBy = "contained")
		private IndexedEntity containing;
	}
}
