package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.analysis.TokenizerFactory;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.engine.BoostStrategy;

public class DynamicFieldBoostMapping {
	
	private final SearchMapping mapping;
	private final EntityDescriptor entity;
	private final Map<String,Object> dynamicFieldBoost;
	private final PropertyDescriptor property;
	
	public DynamicFieldBoostMapping(SearchMapping mapping, Class<? extends BoostStrategy> impl, PropertyDescriptor property, EntityDescriptor entity) {
		this.mapping = mapping;
		this.property = property;
		this.entity = entity;
		dynamicFieldBoost = new HashMap<String, Object>();
		this.property.setDynamicFieldBoost(dynamicFieldBoost);
		dynamicFieldBoost.put("impl", impl);
	}
	
	public FieldMapping field() {
		return new FieldMapping(property, entity, mapping);
	}

	public PropertyMapping property(String name, ElementType type) {
		return new PropertyMapping(name, type, entity, mapping);
	}

	public DateBridgeMapping dateBridge(Resolution resolution) {
		return new DateBridgeMapping(mapping, entity, property, resolution);
	}
	
	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping(name, tokenizerFactory, mapping);
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping(entityType, mapping);
	}

	public CalendarBridgeMapping calendarBridge(Resolution resolution) {
		return new CalendarBridgeMapping(mapping,entity,property, resolution);
	}
	
	public IndexEmbeddedMapping indexEmbedded() {
		return new IndexEmbeddedMapping(mapping,property,entity);
	}

	public ContainedInMapping containedIn() {
		return new ContainedInMapping(mapping, property, entity);
	} 

}
