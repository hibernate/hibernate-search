/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.solr.common.ResourceLoader;
import org.apache.solr.util.plugin.ResourceLoaderAware;

import org.hibernate.search.SearchException;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class HibernateSearchResourceLoader implements ResourceLoader {
	private final String charset;

	public HibernateSearchResourceLoader() {
		this.charset = null;
	}

	public HibernateSearchResourceLoader(String charset) {
		this.charset = charset;
	}

	public InputStream openResource(String resource) throws IOException {
		return FileHelper.openResource( resource );
	}

	public List<String> getLines(String resource) throws IOException {
		final InputStream stream = openResource( resource );
		if ( stream == null ) {
			throw new SearchException( "Resource not found: " + resource );
		}
		try {
			final InputStreamReader charsetAwareReader;
			charsetAwareReader = charset == null ?
					new InputStreamReader( stream ) :
					new InputStreamReader( stream, charset );
			final List<String> results = new ArrayList<String>();
			final BufferedReader reader = new BufferedReader( charsetAwareReader );
			try {
				String line = reader.readLine();
				while ( line != null ) {
					// comment or empty line
					if ( line.length() != 0 && !line.startsWith( "#" ) ) {
						results.add( line );
					}
					line = reader.readLine();
				}
			}
			finally {
				FileHelper.closeResource( reader );
			}
			return Collections.unmodifiableList( results );
		}
		finally {
			FileHelper.closeResource( stream );
		}
	}

	public Object newInstance(String cname, String... subpackages) {
		if ( subpackages != null && subpackages.length > 0 ) {
			throw new UnsupportedOperationException( "newInstance(classname, packages) not implemented" );
		}

		final Object instance = ClassLoaderHelper.instanceFromName(
				Object.class, cname, this.getClass(), "Solr resource"
		);
		if ( instance instanceof ResourceLoaderAware ) {
			( (ResourceLoaderAware) instance ).inform( this );
		}
		return instance;
	}
}
