/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.io.Serializable;
import java.util.Properties;
import java.util.Enumeration;

import org.apache.lucene.document.Document;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.indexes.spi.IndexManager;

/**
 * Used to test the configuration of a third-party strategy
 * @author Sanne Grinovero
 */
public class UselessShardingStrategy implements IndexShardingStrategy {

	@Override
	public IndexManager getIndexManagerForAddition(Class<?> entity, Serializable id, String idInString, Document document) {
		return null;
	}

	@Override
	public IndexManager[] getIndexManagersForAllShards() {
		return null;
	}

	@Override
	public IndexManager[] getIndexManagersForDeletion(Class<?> entity, Serializable id, String idInString) {
		return null;
	}

	@Override
	public IndexManager[] getIndexManagersForQuery(FullTextFilterImplementor[] fullTextFilters) {
		return null;
	}

	@Override
	public void initialize(Properties properties, IndexManager[] indexManagers) {
		Enumeration<?> propertyNames = properties.propertyNames();
		int counter;
		counter = checkEnumeration( propertyNames );
		if ( counter != 2 ) {
			throw new IllegalStateException( "propertyNames() fails" );
		}
		counter = checkEnumeration( properties.keys() );
		if ( counter != 2 ) {
			throw new IllegalStateException( "keys() fails" );
		}
		counter = 0;
		for ( Object key : properties.keySet() ) {
			if ( ! String.class.isInstance( key ) ) {
				continue;
			}
			if ( String.class.cast( key ).startsWith( "test." ) ) {
				System.out.println( key );
				counter++;
			}
		}
		if ( counter != 2 ) {
			throw new IllegalStateException( "keySet() fails" );
		}
	}

	private int checkEnumeration(Enumeration<?> propertyNames) {
		int counter = 0;
		while ( propertyNames.hasMoreElements() ) {
			Object key = propertyNames.nextElement();
			if ( ! String.class.isInstance( key ) ) {
				continue;
			}
			String propertyName = (String) key;
			if ( propertyName.startsWith( "test." ) ) {
				counter++;
			}
		}
		return counter;
	}

}
