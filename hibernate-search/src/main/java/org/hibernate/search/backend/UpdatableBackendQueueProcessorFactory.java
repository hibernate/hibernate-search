package org.hibernate.search.backend;

import java.util.Set;

import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.store.DirectoryProvider;

/**
 * Experimental
 * Allow a BackendQueueProcessorFactory to be notified of DiurectoryProvider changes
 *
 * @author Emmanuel Bernard
 */
public interface UpdatableBackendQueueProcessorFactory extends BackendQueueProcessorFactory {
	/**
	 * Update the list of <code>DirectoryProvider</code>s in case the SearchFactory is updated.
	 * The processor factory should react and update its state accordingly.
	 */
	void updateDirectoryProviders(Set<DirectoryProvider<?>> providers, WorkerBuildContext context);
}
