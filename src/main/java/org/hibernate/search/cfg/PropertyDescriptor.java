package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;

/**
 * @author Emmanuel Bernard
 */
public class PropertyDescriptor {
	private ElementType type;
	private String name;
	private Collection<Map<String, Object>> fields = new ArrayList<Map<String, Object>>();
	private Map<String, Object> documentId;

	public PropertyDescriptor(String name, ElementType type) {
		this.name = name;
		this.type = type;
	}
	
	public void setDocumentId(Map<String, Object> documentId) {
		this.documentId = documentId;
	}

	public void addField(Map<String, Object> field) {
		fields.add( field );
	}

	public Collection<Map<String, Object>> getFields() {
		return fields;
	}

	public Map<String, Object> getDocumentId() {
		return documentId;
	}
}
