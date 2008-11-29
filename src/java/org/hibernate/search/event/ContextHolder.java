// $Id$
package org.hibernate.search.event;

import java.util.WeakHashMap;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.cfg.SearchConfigurationFromHibernateCore;
import org.hibernate.search.impl.SearchFactoryImpl;

/**
 * Holds already built SearchFactory per Hibernate Configuration object
 * concurrent threads do not share this information
 *
 * @author Emmanuel Bernard
 */
public class ContextHolder {
	private static final ThreadLocal<WeakHashMap<Configuration, SearchFactoryImpl>> contexts =
			new ThreadLocal<WeakHashMap<Configuration, SearchFactoryImpl>>();

	//code doesn't have to be multithreaded because SF creation is not.
	//this is not a public API, should really only be used during the SessionFactory building
	public static SearchFactoryImpl getOrBuildSearchFactory(Configuration cfg) {
		WeakHashMap<Configuration, SearchFactoryImpl> contextMap = contexts.get();
		if ( contextMap == null ) {
			contextMap = new WeakHashMap<Configuration, SearchFactoryImpl>( 2 );
			contexts.set( contextMap );
		}
		SearchFactoryImpl searchFactory = contextMap.get( cfg );
		if ( searchFactory == null ) {
			searchFactory = new SearchFactoryImpl( new SearchConfigurationFromHibernateCore( cfg ) );
			contextMap.put( cfg, searchFactory );
		}
		return searchFactory;
	}
}
