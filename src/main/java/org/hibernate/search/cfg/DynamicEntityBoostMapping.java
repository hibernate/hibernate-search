package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.analysis.TokenizerFactory;
import org.hibernate.search.engine.BoostStrategy;

public class DynamicEntityBoostMapping {
	
	private final SearchMapping mapping;
	private final Map<String,Object> dynamicEntityBoost;
	private final EntityDescriptor entity;
	private final EntityMapping entityMapping;
	
	public DynamicEntityBoostMapping(SearchMapping mapping, Class<? extends BoostStrategy> impl, EntityDescriptor entity, EntityMapping entityMapping) {
		this.mapping = mapping;
		this.entity = entity;
		this.entityMapping = entityMapping;
		this.dynamicEntityBoost = new HashMap<String, Object>();
		this.entity.setDynamicEntityBoost(dynamicEntityBoost);
		this.dynamicEntityBoost.put("impl", impl);
		
	}
	
	public FullTextFilterDefMapping fullTextFilterDef(String name, Class<?> impl) {
		return new FullTextFilterDefMapping(mapping,name, impl);
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

	public ClassBridgeMapping classBridge(Class<?> impl) {
		return new ClassBridgeMapping(mapping, entity, impl, entityMapping);
	}
	
}
