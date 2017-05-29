/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.errorhandling;

import static org.junit.Assert.assertSame;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.exception.ErrorContext;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test for making sure error handlers can also be passed as instances rather than via their FQN.
 *
 * @author Gunnar Morling
 */
@TestForIssue(jiraKey = "HSEARCH-1624")
public class ErrorHandlerGivenAsInstanceTest {

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	@Test
	public void canPassErrorHandlerInstanceToConfiguration() {
		// given
		ErrorHandler errorHandler = new MyErrorHandler();

		// when
		SearchConfiguration cfg = getSearchConfiguration( errorHandler );
		SearchIntegrator searchIntegrator = integratorResource.create( cfg );

		// then
		assertSame( errorHandler, searchIntegrator.getErrorHandler() );
	}

	private SearchConfiguration getSearchConfiguration(ErrorHandler errorHandler) {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.getProperties().put( Environment.ERROR_HANDLER, errorHandler );

		return cfg;
	}

	private static class MyErrorHandler implements ErrorHandler {

		@Override
		public void handle(ErrorContext context) {
		}

		@Override
		public void handleException(String errorMsg, Throwable exception) {
		}
	}
}
