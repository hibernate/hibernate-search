/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.massindexing;

import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;

import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.logging.log4j.Level;

class MassIndexingErrorDefaultBackgroundFailureHandlerIT extends AbstractMassIndexingErrorIT {

	@RegisterExtension
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Override
	protected FailureHandler getBackgroundFailureHandlerReference() {
		return null;
	}

	@Override
	protected MassIndexingFailureHandler getMassIndexingFailureHandler() {
		return null;
	}

	@Override
	protected void expectNoFailureHandling() {
		logged.expectLevel( Level.ERROR ).never();
	}

	@Override
	protected void assertNoFailureHandling() {
		// If we get there, everything works fine.
	}

}
