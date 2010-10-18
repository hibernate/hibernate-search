package org.hibernate.search.cfg;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: Gustavo Fernandes
 */
public class NumericFieldMapping {
	private final SearchMapping mapping;
	private final EntityDescriptor entity;
	private final PropertyDescriptor property;
	private final Map<String, Object> numericField = new HashMap<String, Object>();

	public NumericFieldMapping(PropertyDescriptor property, EntityDescriptor entity, SearchMapping mapping) {
		this.property = property;
		this.mapping = mapping;
		this.entity = entity;
		property.addNumericField(numericField);
	}

	public NumericFieldMapping precisionStep(int precisionStep) {
		numericField.put("precisionStep", precisionStep);
		return this;
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping(entityType, mapping);
	}
}
