/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.optimizer;

import java.util.Map;

/**
 * @author Emmanuel Bernard
 */
public class IncrementalOptimizerStrategyPerformanceTest extends OptimizerPerformanceTest {

	@Override
	public void configure(Map<String,Object> cfg) {
		super.configure( cfg );
		cfg.put( "hibernate.search.default.optimizer.transaction_limit.max", "10" );
	}

}
