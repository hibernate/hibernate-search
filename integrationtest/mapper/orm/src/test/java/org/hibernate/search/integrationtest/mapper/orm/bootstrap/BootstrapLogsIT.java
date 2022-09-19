/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.search.engine.Version;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.SystemHelper;
import org.hibernate.search.util.impl.test.SystemHelper.SystemPropertyRestorer;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.ExpectedLog4jLog;

import org.junit.Rule;
import org.junit.Test;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
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
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	public void noSuspiciousLogEvents() {
		logged.expectEvent( suspiciousLogEventMatcher() )
				.never(); // Also fails if a higher severity event (e.g. error) is logged

		backendMock.expectAnySchema( IndexedEntity.NAME );

		ormSetupHelper.start()
				.setup( IndexedEntity.class, ContainedEntity.class );
	}

	@Test
	@SuppressWarnings("deprecation")
	public void version() {
		if ( Version.versionString().equals( "UNKNOWN" ) ) {
			throw new IllegalStateException( "Tests seem to be running from an IDE,"
					+ " or the code was compiled by an IDE."
					+ " This test won't work, because it requires Version.java to be injected with some bytecode"
					+ " through a Maven plugin that IDEs don't know about." );
		}

		String propertyKey = "org.hibernate.search.version";
		String expectedHibernateSearchVersion = System.getProperty( propertyKey );
		if ( expectedHibernateSearchVersion == null ) {
			throw new IllegalStateException( "This test cannot be executed, because system property '"
					+ propertyKey + "' was not defined." );
		}

		logged.expectMessage( "HSEARCH000034",
				"Hibernate Search version", expectedHibernateSearchVersion
		).once();

		backendMock.expectAnySchema( IndexedEntity.NAME );

		ormSetupHelper.start()
				.setup( IndexedEntity.class, ContainedEntity.class );

		// Also check that retrieving the version string explicitly returns the right version string.
		assertThat( Version.versionString() ).isEqualTo( expectedHibernateSearchVersion );
		assertThat( Version.getVersionString() ).isEqualTo( expectedHibernateSearchVersion );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4503")
	public void versionLoggingDisabled() {
		String propertyKey = "org.hibernate.search.version";
		String expectedHibernateSearchVersion = System.getProperty( propertyKey );
		if ( expectedHibernateSearchVersion == null ) {
			throw new IllegalStateException( "This test cannot be executed, because system property '"
					+ propertyKey + "' was not defined." );
		}

		try ( SystemPropertyRestorer systemPropertyChange = SystemHelper.setSystemProperty( "jboss.log-version", "false" ) ) {
			logged.expectMessage( "HSEARCH000034" ).never();
			logged.expectMessage( "Hibernate Search version" ).never();

			backendMock.expectAnySchema( IndexedEntity.NAME );

			ormSetupHelper.start()
					.setup( IndexedEntity.class, ContainedEntity.class );
		}
	}

	private static Matcher<? extends LogEvent> suspiciousLogEventMatcher() {
		return new TypeSafeMatcher<LogEvent>() {
			private final Level level = Level.WARN;

			@Override
			public void describeTo(Description description) {
				description.appendText( "a LogEvent with " ).appendValue( level ).appendText( " level or higher" )
						.appendText( " (ignoring known test-only warnings)" );
			}

			@Override
			protected boolean matchesSafely(LogEvent item) {
				return item.getLevel().isMoreSpecificThan( level )
						// Ignore these, they are warning but are expected (just related to the testing infrastructure)
						&& !( CONNECTION_POOL_WARNING_PATTERN.matcher( item.getMessage().getFormattedMessage() ).find()
								|| HBM2DDL_WARNING_PATTERN.matcher( item.getMessage().getFormattedMessage() ).find() );
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
