package org.hibernate.search.cfg;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Gustavo Fernandes
 */
public class NumericFieldMapping extends FieldMapping {
	private final Map<String, Object> numericField = new HashMap<String, Object>();

	public NumericFieldMapping(PropertyDescriptor property, EntityDescriptor entity, SearchMapping mapping) {
		super(property,entity,mapping);
		property.addNumericField(numericField);
	}

	public NumericFieldMapping precisionStep(int precisionStep) {
		numericField.put("precisionStep", precisionStep);
		return this;
	}

}
