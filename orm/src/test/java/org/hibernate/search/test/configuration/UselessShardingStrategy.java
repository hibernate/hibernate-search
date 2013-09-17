/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
