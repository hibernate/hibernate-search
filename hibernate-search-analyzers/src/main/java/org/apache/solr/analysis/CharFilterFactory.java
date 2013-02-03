/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.apache.solr.analysis;

import java.io.Reader;
import java.util.Set;

@Deprecated
public abstract class CharFilterFactory extends org.apache.lucene.analysis.util.CharFilterFactory {

	protected final org.apache.lucene.analysis.util.CharFilterFactory delegate;

	private CharFilterFactory(org.apache.lucene.analysis.util.CharFilterFactory delegate) {
		this.delegate = delegate;
	}

	public static CharFilterFactory forName(String name) {
		return new CharFilterFactory(org.apache.lucene.analysis.util.CharFilterFactory.forName( name )) {
			@Override
			public Reader create(Reader input) {
				return this.delegate.create( input );
			}
			
		};
	}

	/** looks up a charfilter class by name from context classpath */
	public static Class<? extends CharFilterFactory> lookupClass(String name) {
		throw new RuntimeException("can't provide a resonable alternative for this implementation");
	}

	/** returns a list of all available charfilter names */
	public static Set<String> availableCharFilters() {
		return org.apache.lucene.analysis.util.CharFilterFactory.availableCharFilters();
	}

	/**
	 * Reloads the factory list from the given {@link ClassLoader}.
	 * Changes to the factories are visible after the method ends, all
	 * iterators ({@link #availableCharFilters()},...) stay consistent.
	 * 
	 * <p>
	 * <b>NOTE:</b> Only new factories are added, existing ones are never removed or replaced.
	 * 
	 * <p>
	 * <em>This method is expensive and should only be called for discovery
	 * of new factories on the given classpath/classloader!</em>
	 */
	public static void reloadCharFilters(ClassLoader classloader) {
		org.apache.lucene.analysis.util.CharFilterFactory.reloadCharFilters( classloader );
	}

}
