package org.hibernate.search.cfg;

import java.util.HashMap;
import java.util.Map;

import org.apache.solr.analysis.TokenizerFactory;
import org.apache.solr.analysis.TokenFilterFactory;

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
		mapping.addAnalyzerDef(analyzerDef);
		analyzerDef.put( "name", name );
		tokenizer = new HashMap<String, Object>();
		tokenizer.put( "factory", tokenizerFactory );
		analyzerDef.put( "tokenizer", tokenizer );
	}

	/**
	 * @TokenizerDef(, ... params={@Parameter(name="name", value="value"), ...})
	 */
	public AnalyzerDefMapping tokenizerParam(String name, String value) {
		Map<String, Object> param = SearchMapping.addElementToAnnotationArray(tokenizer, "params");
		param.put("name", name);
		param.put("value", value);
		return this;
	}

	/**
	 * @TokenFilterDef(factory=factory)
	 */
	public TokenFilterDefMapping filter(Class<? extends TokenFilterFactory> factory) {
		return new TokenFilterDefMapping(factory, analyzerDef, mapping);
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping(name, tokenizerFactory, mapping);
	}

	public EntityMapping indexedClass(Class<?> entityType) {
		return new EntityMapping(entityType, null, mapping);
	}

	public EntityMapping indexedClass(Class<?> entityType, String indexName) {
		return new EntityMapping(entityType, indexName,  mapping);
	}
}
