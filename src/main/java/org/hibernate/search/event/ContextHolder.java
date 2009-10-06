/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
