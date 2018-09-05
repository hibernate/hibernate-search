/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.scenario;

import java.io.IOException;
import java.util.Properties;

/**
 * @author Tomas Hradec
 */
public abstract class AbstractTestScenario implements TestScenario {

	public Properties getHibernateProperties() {
		Properties properties = new Properties();
		properties.setProperty( "hibernate.hbm2ddl.auto", "create" );
		properties.setProperty( "hibernate.search.default.lucene_version", "LUCENE_CURRENT" );
		return properties;
	}

	@Override
	public final void run(TestContext testContext) throws IOException {
		initContext( testContext );
		new TestExecutor().run(
				testContext,
				createWarmupContext( testContext ),
				createMeasureContext( testContext )
		);
	}

	protected void initContext(TestContext testContext) {
		// Default: do nothing
	}

	private TestScenarioContext createWarmupContext(TestContext testContext) {
		TestScenarioContext ctx = new TestScenarioContext( testContext, this );
		addTasks( ctx );
		return ctx;
	}

	private TestScenarioContext createMeasureContext(TestContext testContext) {
		TestScenarioContext ctx = new TestScenarioContext( testContext, this );
		addTasks( ctx );
		return ctx;
	}

	protected abstract void addTasks(TestScenarioContext ctx);

}
