/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class ExpectedLog4jLogTestRule extends ExpectedLog4jLog implements TestRule {

	public static ExpectedLog4jLogTestRule create() {
		return new ExpectedLog4jLogTestRule( DEFAULT_LOGGER_NAME );
	}

	public ExpectedLog4jLogTestRule(String loggerName) {
		super( loggerName );
	}

	@Override
	public Statement apply(Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				ReusableOrmSetupHolder.TestRuleExtensionContext context =
						new ReusableOrmSetupHolder.TestRuleExtensionContext( description );
				try {
					beforeEach( context );
					base.evaluate();
				}
				finally {
					afterEach( context );
				}
			}
		};
	}
}
