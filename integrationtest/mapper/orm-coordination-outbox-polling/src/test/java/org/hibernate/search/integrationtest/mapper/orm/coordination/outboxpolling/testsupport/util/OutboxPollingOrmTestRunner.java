/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.testsupport.util;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/**
 * Helper for running specific Hibernate ORM integration tests with outbox-polling from the IDE.
 * <p>
 * Adapt the classpath filter as needed to run a single test or an entire test package.
 */
public class OutboxPollingOrmTestRunner {
	private OutboxPollingOrmTestRunner() {
		// To make checkstyle happy
	}

	public static void main(String[] args) {
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
				// Use specific package the test lives in:
				.selectors( selectPackage( "org.hibernate.search.integrationtest.mapper.orm.automaticindexing" ) )
				// additionally filter by classnames to reduce the number of executed tests:
//				.filters( includeClassNamePatterns( "AutomaticIndexingBasicIT" ) )
				// See DiscoverySelectors for all possible selectors.
				.build();

		SummaryGeneratingListener listener = new SummaryGeneratingListener();

		Launcher launcher = LauncherFactory.create();
		launcher.registerTestExecutionListeners( listener );
		launcher.execute( request );

		listener.getSummary().printTo( new PrintWriter( System.out, true, StandardCharsets.UTF_8 ) );
	}
}
