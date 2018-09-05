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
public interface TestScenario {

	Properties getHibernateProperties();

	void run(TestContext testContext) throws IOException;

}
