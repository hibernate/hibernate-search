// $Id$
package org.hibernate.search.store;

import java.util.Properties;
import java.io.Serializable;

import org.apache.lucene.document.Document;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.filter.FullTextFilterImplementor;

/**
 * @author Emmanuel Bernard
 */
public class NotShardedStrategy implements IndexShardingStrategy {
	private DirectoryProvider<?>[] directoryProvider;
	public void initialize(Properties properties, DirectoryProvider<?>[] providers) {
		this.directoryProvider = providers;
		if ( directoryProvider.length > 1) {
			throw new AssertionFailure("Using SingleDirectoryProviderSelectionStrategy with multiple DirectryProviders");
		}
	}

	public DirectoryProvider<?>[] getDirectoryProvidersForAllShards() {
		return directoryProvider;
	}

	public DirectoryProvider<?> getDirectoryProviderForAddition(Class<?> entity, Serializable id, String idInString, Document document) {
		return directoryProvider[0];
	}

	public DirectoryProvider<?>[] getDirectoryProvidersForDeletion(Class<?> entity, Serializable id, String idInString) {
		return directoryProvider;
	}

	public DirectoryProvider<?>[] getDirectoryProvidersForQuery(FullTextFilterImplementor[] fullTextFilters) {
		return directoryProvider;
	}

}
