/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.errorhandling;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.testsupport.TestForIssue;

/**
 * Test for making sure error handlers can also be passed as instances rather than via their FQN.
 *
 * @author Gunnar Morling
 */
@TestForIssue(jiraKey = "HSEARCH-1624")
public class ErrorHandlerGivenAsInstanceTest extends ErrorHandlingDuringDocumentCreationTest {

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.getProperties().put( Environment.ERROR_HANDLER, new MockErrorHandler() );
	}
}
