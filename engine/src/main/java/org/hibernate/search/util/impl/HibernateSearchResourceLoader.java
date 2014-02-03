/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.util.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.hibernate.search.SearchException;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class HibernateSearchResourceLoader implements ResourceLoader {

	private final ClassLoader classLoader;

	/**
	 * Used only temporarily to keep the number of changes limited in the scope of
	 * Lucene 4 migration. TODO HSEARCH-1456 ClassLoader instances should be passed explicitly.
	 */
	@Deprecated
	public HibernateSearchResourceLoader() {
		this.classLoader = HibernateSearchResourceLoader.class.getClassLoader();
	}

	public HibernateSearchResourceLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public InputStream openResource(String resource) throws IOException {
		InputStream inputStream = FileHelper.openResource( resource );
		if ( inputStream == null ) {
			throw new SearchException( "Resource not found: " + resource );
		}
		else {
			return inputStream;
		}
	}

	@Override
	public <T> Class<? extends T> findClass(String cname, Class<T> expectedType) {
		return ClassLoaderHelper.classForName( expectedType, cname, classLoader, describeComponent( cname) );
	}

	@Override
	public <T> T newInstance(String cname, Class<T> expectedType) {
		return ClassLoaderHelper.instanceFromName( expectedType, cname, classLoader, describeComponent( cname) );
	}

	private static String describeComponent(final String classname) {
		return "Lucene Analyzer component " + classname;
	}

}
