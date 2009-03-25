// $Id$
package org.hibernate.search.store.optimization;

import java.util.Properties;

import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.backend.Workspace;

/**
 * @author Emmanuel Bernard
 */
public class NoOpOptimizerStrategy implements OptimizerStrategy {
	public void initialize(DirectoryProvider directoryProvider, Properties indexProperties, SearchFactoryImplementor searchFactoryImplementor) {
	}

	public void optimizationForced() {
	}

	public boolean needOptimization() {
		return false;
	}

	public void addTransaction(long operations) {
	}

	public void optimize(Workspace workspace) {
	}
}
