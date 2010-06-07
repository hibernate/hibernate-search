package org.hibernate.search;

import org.hibernate.search.backend.LuceneIndexingParameters;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.optimization.OptimizerStrategy;

/**
 * Build context where new built element can be registered.
 * 
 * @author Emmanuel Bernard
 */
public interface WritableBuildContext extends BuildContext {
	void addOptimizerStrategy(DirectoryProvider<?> provider, OptimizerStrategy optimizerStrategy);

	void addIndexingParameters(DirectoryProvider<?> provider, LuceneIndexingParameters indexingParams);

	void addClassToDirectoryProvider(Class<?> entity, DirectoryProvider<?> directoryProvider, boolean exclusiveIndexUsage);
}