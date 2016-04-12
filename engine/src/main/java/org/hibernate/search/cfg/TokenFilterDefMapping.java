/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg;

import java.util.Map;

import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;

/**
 * @author Emmanuel Bernard
 */
public class TokenFilterDefMapping {
	private Map<String, Object> filter;
	private Map<String, Object> analyzerDef;
	private SearchMapping mapping;

	TokenFilterDefMapping(Class<? extends TokenFilterFactory> factory, Map<String, Object> analyzerDef, SearchMapping mapping) {
		this.mapping = mapping;
		this.analyzerDef = analyzerDef;
		this.filter = SearchMapping.addElementToAnnotationArray( analyzerDef, "filters" );
		filter.put( "factory", factory );
	}

	/**
	 * {@code &#064;TokenFilterDef(, ... params={&#064;Parameter(name="name", value="value"), ...} }
	 * @param name the name of the parameter
	 * @param value the value of the parameter
	 * @return {@code this} for method chaining
	 */
	public TokenFilterDefMapping param(String name, String value) {
		Map<String, Object> param = SearchMapping.addElementToAnnotationArray( filter, "params" );
		param.put( "name", name );
		param.put( "value", value );
		return this;
	}

	/**
	 * {@code &#064;TokenFilterDef(factory=factory)}
	 * @param factory the {@link TokenFilterFactory}
	 * @return a new {@link TokenFilterDefMapping}
	 */
	public TokenFilterDefMapping filter(Class<? extends TokenFilterFactory> factory) {
		return new TokenFilterDefMapping( factory, analyzerDef, mapping );
	}

	/**
	 * {@code &#064;CharFilterDef(factory=factory) }
	 * @param factory the {@link CharFilterFactory}
	 * @return a new {@link CharFilterDefMapping}
	 */
	public CharFilterDefMapping charFilter(Class<? extends CharFilterFactory> factory) {
		return new CharFilterDefMapping( factory, analyzerDef, mapping );
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping(entityType, mapping);
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping( name, tokenizerFactory, mapping );
	}

	public FullTextFilterDefMapping fullTextFilterDef(String name, Class<?> impl) {
		return new FullTextFilterDefMapping( mapping, name, impl );
	}

}
