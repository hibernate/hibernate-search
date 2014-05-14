/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.util.TokenizerFactory;
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
