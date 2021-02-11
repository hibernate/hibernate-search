/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.localheapqueue;

import org.hibernate.search.util.impl.integrationtest.mapper.orm.AutomaticIndexingStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.automaticindexing.LocalHeapQueueAutomaticIndexingStrategy;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;

/**
 * Tests suite for automatic indexing with {@link LocalHeapQueueAutomaticIndexingStrategy}.
 * <p>
 * The main purpose of this test suite is to check that the concept of automatic indexing in a background process can work,
 * but {@link LocalHeapQueueAutomaticIndexingStrategy} definitely isn't something we can use in a production environment,
 * most notably because of its lack of persistence of the event queue.
 * <p>
 * Production-ready automatic indexing strategies involving event queues will need to be tested
 * both with a suite like this one, and much more extensive tests for all the edge cases
 * (ordering of events within a transaction, persistence of the queue, sharding if relevant, ...).
 */
@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({
		// Just execute all automatic indexing tests
		"org.hibernate.search.integrationtest.mapper.orm.automaticindexing..*",
		// ... except these tests that just cannot work with the local-heap queue strategy
		// > Synchronization strategies can only be used with the "session" automatic indexing strategy
		"!org.hibernate.search.integrationtest.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyIT",
		// > Sending events outside of transactions, during a flush, doesn't work for some reason;
		//   entities are only visible from other sessions after the original session is closed.
		"!org.hibernate.search.integrationtest.mapper.orm.automaticindexing.session.AutomaticIndexingOutOfTransactionIT"
})
public class LocalHeapQueueAutomaticIndexingStrategyBaseIT {

	@BeforeClass
	public static void beforeAll() {
		// Force the automatic indexing strategy
		OrmSetupHelper.automaticIndexingStrategyExpectations( AutomaticIndexingStrategyExpectations.async(
				LocalHeapQueueAutomaticIndexingStrategy.class.getName(), ".*Local heap queue.*" ) );
	}

	@AfterClass
	public static void afterAll() {
		OrmSetupHelper.automaticIndexingStrategyExpectations( AutomaticIndexingStrategyExpectations.defaults() );
	}

	// For checkstyle.
	public void thisIsNotAUtilityClass() {
	}

}
