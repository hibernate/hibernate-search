package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.ArrayList;

/**
 * @author Emmanuel Bernard
 */
public class PropertyDescriptor {
	private ElementType type;
	private String name;
	private Collection<Map<String, Object>> fields = new ArrayList<Map<String, Object>>();

	public PropertyDescriptor(String name, ElementType type) {
		this.name = name;
		this.type = type;
	}

	public void addField(Map<String, Object> field) {
		fields.add( field );
	}

	public Collection<Map<String, Object>> getFields() {
		return fields;
	}
}
