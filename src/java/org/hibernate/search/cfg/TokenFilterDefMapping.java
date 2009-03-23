package org.hibernate.search.cfg;

import java.util.Map;

import org.apache.solr.analysis.TokenFilterFactory;
import org.apache.solr.analysis.TokenizerFactory;

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
	 * @TokenFilterDef(, ... params={@Parameter(name="name", value="value"), ...})
	 */
	public TokenFilterDefMapping param(String name, String value) {
		Map<String, Object> param = SearchMapping.addElementToAnnotationArray(filter, "params");
		param.put("name", name);
		param.put("value", value);
		return this;
	}

	/**
	 * @TokenFilterDef(factory=factory)
	 */
	public TokenFilterDefMapping filter(Class<? extends TokenFilterFactory> factory) {
		return new TokenFilterDefMapping(factory, analyzerDef, mapping );
	}

	public EntityMapping indexedClass(Class<?> entityType) {
		return new EntityMapping(entityType, null,  mapping);
	}

	public EntityMapping indexedClass(Class<?> entityType, String indexName) {
		return new EntityMapping(entityType, indexName,  mapping);
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping(name, tokenizerFactory, mapping);
	}

}
