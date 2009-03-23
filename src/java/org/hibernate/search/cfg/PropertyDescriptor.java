package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Emmanuel Bernard
 */
public class PropertyDescriptor {
	private ElementType type;
	private String name;
	private Set<Map<String, Object>> fields = new HashSet<Map<String, Object>>();

	public PropertyDescriptor(String name, ElementType type) {
		this.name = name;
		this.type = type;
	}

	public void addField(Map<String, Object> field) {
		fields.add( field );
	}

	public Set<Map<String, Object>> getFields() {
		return fields;
	}
}
