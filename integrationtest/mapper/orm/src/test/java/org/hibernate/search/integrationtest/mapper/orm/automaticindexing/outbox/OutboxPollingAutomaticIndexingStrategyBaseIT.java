/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.outbox;

import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyNames;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.AutomaticIndexingStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;

/**
 * Tests suite for automatic indexing with {@link AutomaticIndexingStrategyNames#OUTBOX_POLLING}.
 */
@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({
		// Just execute all automatic indexing tests
		"org.hibernate.search.integrationtest.mapper.orm.automaticindexing..*",
		// ... except these tests that just cannot work with the outbox table strategy
		// > Synchronization strategies can only be used with the "session" automatic indexing strategy
		"!org.hibernate.search.integrationtest.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyIT",
		// > Sending events outside of transactions, during a flush, doesn't work for some reason;
		//   entities are only visible from other sessions after the original session is closed.
		"!org.hibernate.search.integrationtest.mapper.orm.automaticindexing.session.AutomaticIndexingOutOfTransactionIT",
		// > tested in a different class: OutboxAutomaticIndexingBridgeExplicitReindexingFunctionalIT
		"!org.hibernate.search.integrationtest.mapper.orm.automaticindexing.bridge.AutomaticIndexingBridgeExplicitReindexingFunctionalIT",
		// > already executed with the outbox table strategy
		"!org.hibernate.search.integrationtest.mapper.orm.automaticindexing.outboxtable..*"
})
public class OutboxPollingAutomaticIndexingStrategyBaseIT {

	@BeforeClass
	public static void beforeAll() {
		// Force the automatic indexing strategy
		OrmSetupHelper.defaultAutomaticIndexingStrategy( AutomaticIndexingStrategyExpectations.outboxPolling() );
	}

	@AfterClass
	public static void afterAll() {
		OrmSetupHelper.defaultAutomaticIndexingStrategy( AutomaticIndexingStrategyExpectations.defaults() );
	}

	// For checkstyle.
	public void thisIsNotAUtilityClass() {
	}

}
