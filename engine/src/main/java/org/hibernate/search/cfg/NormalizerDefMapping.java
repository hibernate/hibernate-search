/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;

/**
 * @author Emmanuel Bernard
 */
public class NormalizerDefMapping {
	private SearchMapping mapping;
	private Map<String, Object> definition;

	NormalizerDefMapping(String name, SearchMapping mapping) {
		this.mapping = mapping;
		this.definition = new HashMap<String, Object>();
		mapping.addNormalizerDef( definition );
		definition.put( "name", name );
	}

	/**
	 * {@code &#064;CharFilterDef(factory=factory) }
	 * @param factory the {@link CharFilterFactory}
	 * @return a new {@link CharFilterDefMapping}
	 */
	public CharFilterDefMapping charFilter(Class<? extends CharFilterFactory> factory) {
		return charFilter( "", factory );
	}

	/**
	 * {@code &#064;CharFilterDef(name=name, factory=factory) }
	 * @param name the char filter name
	 * @param factory the {@link CharFilterFactory}
	 * @return a new {@link CharFilterDefMapping}
	 */
	public CharFilterDefMapping charFilter(String name, Class<? extends CharFilterFactory> factory) {
		return new CharFilterDefMapping( name, factory, definition, mapping );
	}

	/**
	 * {@code &#064;TokenFilterDef(factory=factory) }
	 * @param factory the {@link TokenFilterFactory}
	 * @return a new {@link TokenFilterDefMapping}
	 */
	public TokenFilterDefMapping filter(Class<? extends TokenFilterFactory> factory) {
		return filter( "", factory );
	}

	/**
	 * {@code &#064;TokenFilterDef(name=name, factory=factory) }
	 * @param name the token filter name
	 * @param factory the {@link TokenFilterFactory}
	 * @return a new {@link TokenFilterDefMapping}
	 */
	public TokenFilterDefMapping filter(String name, Class<? extends TokenFilterFactory> factory) {
		return new TokenFilterDefMapping( factory, definition, mapping );
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return analyzerDef( name, "", tokenizerFactory );
	}

	public AnalyzerDefMapping analyzerDef(String name, String tokenizerName, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping( name, tokenizerName, tokenizerFactory, mapping );
	}

	public NormalizerDefMapping normalizerDef(String name) {
		return new NormalizerDefMapping( name, mapping );
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping( entityType, mapping );
	}

}
