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
package org.hibernate.search.cfg;

import java.util.HashMap;
import java.util.Map;

import org.apache.solr.analysis.TokenizerFactory;
import org.hibernate.search.annotations.FilterCacheModeType;

/**
 * Mapping class responsible for collecting data for constructing
 * FullTextFilterDef annotation.
 */
public class FullTextFilterDefMapping {

	private final SearchMapping mapping;
	private final Map<String,Object> fullTextFilterDef;

	public FullTextFilterDefMapping(SearchMapping mapping, String name, Class<?> impl) {
		this.mapping = mapping;
		this.fullTextFilterDef = new HashMap<String, Object>();
		this.fullTextFilterDef.put( "name", name );
		this.fullTextFilterDef.put( "impl", impl );
		mapping.addFulltextFilterDef( fullTextFilterDef );
	}

	/**
	 * Add cache implementation for fulltextfilterdef mapping
	 *
	 * @param cache a {@link org.hibernate.search.annotations.FilterCacheModeType} object.
	 * @return FullTextFilterDefMapping
	 */
	public FullTextFilterDefMapping cache(FilterCacheModeType cache) {
		this.fullTextFilterDef.put( "cache", cache );
		return this;
	}

	public FullTextFilterDefMapping fullTextFilterDef(String name, Class<?> impl) {
		return new FullTextFilterDefMapping( mapping, name, impl );
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping( name, tokenizerFactory, mapping );
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping( entityType, mapping );
	}
}
