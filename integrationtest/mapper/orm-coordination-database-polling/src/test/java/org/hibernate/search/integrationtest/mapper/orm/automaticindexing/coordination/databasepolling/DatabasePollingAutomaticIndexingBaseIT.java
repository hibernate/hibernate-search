/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.coordination.databasepolling;

import org.hibernate.search.mapper.orm.coordination.CoordinationStrategyNames;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;

/**
 * Tests suite for automatic indexing with {@link CoordinationStrategyNames#DATABASE_POLLING}.
 */
@RunWith(ClasspathSuite.class)
@ClasspathSuite.IncludeJars(true)
@ClasspathSuite.ClassnameFilters({
		// Just execute all automatic indexing tests
		"org.hibernate.search.integrationtest.mapper.orm.automaticindexing..*",
		// ... except tests designed for a particular coordination strategy:
		"!org.hibernate.search.integrationtest.mapper.orm.automaticindexing.coordination..*",
		// ... and except these tests that just cannot work with the outbox table strategy:
		// > Synchronization strategies can only be used with the "session" automatic indexing strategy
		"!org.hibernate.search.integrationtest.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyIT",
		// > Sending events outside of transactions, during a flush, doesn't work for some reason;
		//   entities are only visible from other sessions after the original session is closed.
		"!org.hibernate.search.integrationtest.mapper.orm.automaticindexing.session.AutomaticIndexingOutOfTransactionIT",
		// > We do not send events for the creation of contained entities,
		//   and as a result one particular use case involving queries instead of associations
		//   cannot work.
		//   We will address that someday with explicit support for queries;
		//   see https://hibernate.atlassian.net/browse/HSEARCH-1937 .
		"!org.hibernate.search.integrationtest.mapper.orm.automaticindexing.bridge.AutomaticIndexingBridgeExplicitReindexingFunctionalIT"
})
public class DatabasePollingAutomaticIndexingBaseIT {

	@BeforeClass
	public static void beforeAll() {
		// Force the automatic indexing strategy
		OrmSetupHelper.defaultAutomaticIndexingStrategy( CoordinationStrategyExpectations.outboxPolling() );
	}

	@AfterClass
	public static void afterAll() {
		OrmSetupHelper.defaultAutomaticIndexingStrategy( CoordinationStrategyExpectations.defaults() );
	}

	// For checkstyle.
	public void thisIsNotAUtilityClass() {
	}

}
