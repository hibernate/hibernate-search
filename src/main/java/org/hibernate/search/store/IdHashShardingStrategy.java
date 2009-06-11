// $Id$
package org.hibernate.search.store;

import java.util.Properties;
import java.io.Serializable;

import org.apache.lucene.document.Document;

import org.hibernate.search.filter.FullTextFilterImplementor;

/**
 * This implementation use idInString as the hashKey.
 * 
 * @author Emmanuel Bernard
 */
public class IdHashShardingStrategy implements IndexShardingStrategy {
	
	private DirectoryProvider<?>[] providers;
	public void initialize(Properties properties, DirectoryProvider<?>[] providers) {
		this.providers = providers;
	}

	public DirectoryProvider<?>[] getDirectoryProvidersForAllShards() {
		return providers;
	}

	public DirectoryProvider<?> getDirectoryProviderForAddition(Class<?> entity, Serializable id, String idInString, Document document) {
		return providers[ hashKey(idInString) ];
	}

	public DirectoryProvider<?>[] getDirectoryProvidersForDeletion(Class<?> entity, Serializable id, String idInString) {
		if ( idInString == null ) return providers;
		return new DirectoryProvider[] { providers[hashKey( idInString )] };
	}

	public DirectoryProvider<?>[] getDirectoryProvidersForQuery(FullTextFilterImplementor[] fullTextFilters) {
		return getDirectoryProvidersForAllShards();
	}

	private int hashKey(String key) {
		// reproduce the hashCode implementation of String as documented in the javadoc
		// to be safe cross Java version (in case it changes some day)
		int hash = 0;
		int length = key.length();
		for ( int index = 0; index < length; index++ ) {
			hash = 31 * hash + key.charAt( index );
		}
		return Math.abs( hash % providers.length );
	}
}
