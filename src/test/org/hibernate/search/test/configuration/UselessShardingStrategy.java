package org.hibernate.search.test.configuration;

import java.io.Serializable;
import java.util.Properties;

import org.apache.lucene.document.Document;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * Used to test the configuration of a third-party strategy
 * @author Sanne Grinovero
 */
public class UselessShardingStrategy implements IndexShardingStrategy {
	
	public DirectoryProvider getDirectoryProviderForAddition(Class entity, Serializable id, String idInString, Document document) {
		return null;
	}

	public DirectoryProvider[] getDirectoryProvidersForAllShards() {
		return null;
	}

	public DirectoryProvider[] getDirectoryProvidersForDeletion(Class entity, Serializable id, String idInString) {
		return null;
	}

	public void initialize(Properties properties, DirectoryProvider[] providers) {
	}

}
