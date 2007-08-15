//$Id$
package org.hibernate.search.test.optimizer;

import java.io.File;

import org.hibernate.search.store.FSDirectoryProvider;
import org.hibernate.search.Environment;
import org.apache.lucene.analysis.StopAnalyzer;

/**
 * @author Emmanuel Bernard
 */
public class IncrementalOptimizerStrategyTest extends OptimizerTestCase {
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.optimizer.transaction_limit.max", "10" );
	}
}
