/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.impl;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Martin Braun
 */
public class SearchFactoryRegistry {

	public static final String NAME_PROPERTY = "org.hibernate.search.genericjpa.searchfactory.name";
	public static final String DEFAULT_NAME = "default";
	private static Map<String, JPASearchFactoryAdapter> searchFactories = new HashMap<>();

	// FIXME: is this okay for multiple classloaders?

	private SearchFactoryRegistry() {
		// can't touch this!
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static String getNameProperty(Map properties) {
		return (String) properties.getOrDefault( NAME_PROPERTY, DEFAULT_NAME );
	}

	public static JPASearchFactoryAdapter getSearchFactory(String name) {
		return SearchFactoryRegistry.searchFactories.get( name );
	}

	public static void setup(String name, JPASearchFactoryAdapter searchFactory) {
		if ( !SearchFactoryRegistry.searchFactories.containsKey( name ) ) {
			SearchFactoryRegistry.searchFactories.put( name, searchFactory );
		}
	}

	static void unsetup(String name, JPASearchFactoryAdapter searchFactory) {
		SearchFactoryRegistry.searchFactories.remove( name, searchFactory );
	}

}
