// $Id$
package org.hibernate.search.test.configuration;

import java.io.Serializable;
import java.util.Properties;
import java.util.Enumeration;

import org.apache.lucene.document.Document;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.filter.FullTextFilterImplementor;

/**
 * Used to test the configuration of a third-party strategy
 * @author Sanne Grinovero
 */
public class UselessShardingStrategy implements IndexShardingStrategy {
	
	public DirectoryProvider<?> getDirectoryProviderForAddition(Class<?> entity, Serializable id, String idInString, Document document) {
		return null;
	}

	public DirectoryProvider<?>[] getDirectoryProvidersForAllShards() {
		return null;
	}

	public DirectoryProvider<?>[] getDirectoryProvidersForDeletion(Class<?> entity, Serializable id, String idInString) {
		return null;
	}

	public DirectoryProvider<?>[] getDirectoryProvidersForQuery(FullTextFilterImplementor[] fullTextFilters) {
		return null;
	}

	public void initialize(Properties properties, DirectoryProvider<?>[] providers) {
		Enumeration<?> propertyNames = properties.propertyNames();
		int counter;
		counter = checkEnumeration( propertyNames );
		if (counter != 2) throw new IllegalStateException( "propertyNames() fails" );
		counter = checkEnumeration( properties.keys() );
		if (counter != 2) throw new IllegalStateException( "keys() fails" );
		counter = 0;
		for (Object key :  properties.keySet() ) {
			if ( ! String.class.isInstance( key ) ) continue;
			if ( String.class.cast( key ).startsWith("test.") ) {
				System.out.println( key );
				counter++;
			}
		}
		if (counter != 2) throw new IllegalStateException( "keySet() fails" );		
	}

	private int checkEnumeration(Enumeration<?> propertyNames) {
		int counter = 0;
		while ( propertyNames.hasMoreElements() ) {
			Object key = propertyNames.nextElement();
			if ( ! String.class.isInstance( key ) ) continue;
			String propertyName = (String) key;
			if ( propertyName.startsWith("test.") ) {
				counter++;
			}
		}
		return counter;
	}

}
