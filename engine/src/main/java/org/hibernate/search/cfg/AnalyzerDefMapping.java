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
import org.apache.lucene.analysis.util.TokenFilterFactory;

/**
 * @author Emmanuel Bernard
 */
public class AnalyzerDefMapping {
	private SearchMapping mapping;
	private Map<String, Object> analyzerDef;
	private Map<String, Object> tokenizer;

	AnalyzerDefMapping(String name, Class<? extends TokenizerFactory> tokenizerFactory, SearchMapping mapping) {
		this.mapping = mapping;
		this.analyzerDef = new HashMap<String, Object>();
		mapping.addAnalyzerDef( analyzerDef );
		analyzerDef.put( "name", name );
		tokenizer = new HashMap<String, Object>();
		tokenizer.put( "factory", tokenizerFactory );
		analyzerDef.put( "tokenizer", tokenizer );
	}

	/**
	 * {@code &#064;TokenizerDef(, ... params={&#064;Parameter(name="name", value="value"), ...}) }
	 */
	public AnalyzerDefMapping tokenizerParam(String name, String value) {
		Map<String, Object> param = SearchMapping.addElementToAnnotationArray( tokenizer, "params" );
		param.put( "name", name );
		param.put( "value", value );
		return this;
	}

	/**
	 * {@code &#064;TokenFilterDef(factory=factory) }
	 */
	public TokenFilterDefMapping filter(Class<? extends TokenFilterFactory> factory) {
		return new TokenFilterDefMapping( factory, analyzerDef, mapping );
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping( name, tokenizerFactory, mapping );
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping( entityType, mapping );
	}

}
