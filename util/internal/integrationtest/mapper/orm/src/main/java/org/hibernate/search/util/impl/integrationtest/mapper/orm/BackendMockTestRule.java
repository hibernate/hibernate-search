/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class BackendMockTestRule extends BackendMock implements TestRule {

	public static BackendMockTestRule createGlobal() {
		return new BackendMockTestRule();
	}

	private BackendMockTestRule() {
		super();
	}

	@Override
	public Statement apply(Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				ReusableOrmSetupHolder.TestRuleExtensionContext context =
						new ReusableOrmSetupHolder.TestRuleExtensionContext( description );
				try {
					beforeAll( context );
					base.evaluate();
				}
				finally {
					afterAll( context );
				}
			}
		};
	}
}
