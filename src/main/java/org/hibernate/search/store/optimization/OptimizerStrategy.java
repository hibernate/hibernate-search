// $Id$
package org.hibernate.search.store.optimization;

import java.util.Properties;

import org.hibernate.search.backend.Workspace;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.store.DirectoryProvider;

/**
 * @author Emmanuel Bernard
 */
public interface OptimizerStrategy {
	public void initialize(DirectoryProvider directoryProvider, Properties indexProperties, SearchFactoryImplementor searchFactoryImplementor);

	/**
	 * has to be called in a thread safe way
	 */
	void optimizationForced();

	/**
	 * has to be called in a thread safe way
	 */
	boolean needOptimization();

	/**
	 * has to be called in a thread safe way
	 */
	public void addTransaction(long operations);

	/**
	 * has to be called in a thread safe way
	 */
	void optimize(Workspace workspace);

}
