package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
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
	private final EntityDescriptor entity;
	
	public FullTextFilterDefMapping(SearchMapping mapping, EntityDescriptor entity, String name, Class<?> impl) {
		this.mapping = mapping;
		this.entity = entity;
		this.fullTextFilterDef =new HashMap<String, Object>();
		this.fullTextFilterDef.put("name", name);
		this.fullTextFilterDef.put("impl", impl);
		entity.addFulltextFilterDef(fullTextFilterDef);
	}
	
	/**
	 * Add cache implementation for fulltextfilterdef mapping
	 * @param cache
	 * @return FullTextFilterDefMapping
	 */
	public FullTextFilterDefMapping cache(FilterCacheModeType cache) {
		this.fullTextFilterDef.put("cache", cache);
		return this;
	}

	public FullTextFilterDefMapping fullTextFilterDef(String name, Class<?> impl) {
		return new FullTextFilterDefMapping(mapping,entity,name, impl);
	}

	public PropertyMapping property(String name, ElementType type) {
		return new PropertyMapping(name, type, entity, mapping);
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping(name, tokenizerFactory, mapping);
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping(entityType, mapping);
	}

	public ProvidedIdMapping providedId() {
		return new ProvidedIdMapping(mapping,entity);
	}

	
}
