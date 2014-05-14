/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.javaserialization.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class Add implements Operation {
	private String entityClassName;
	private Serializable id;
	private Map<String,String> fieldToAnalyzerMap = new HashMap<String, String>();
	private SerializableDocument document;

	public Add(String entityClassName, Serializable id, SerializableDocument document, Map<String,String> fieldToAnalyzerMap) {
		this.entityClassName = entityClassName;
		this.id = id;
		this.document = document;
		this.fieldToAnalyzerMap = fieldToAnalyzerMap;
	}

	public String getEntityClassName() {
		return entityClassName;
	}

	public Serializable getId() {
		return id;
	}

	public Map<String, String> getFieldToAnalyzerMap() {
		return fieldToAnalyzerMap;
	}

	public SerializableDocument getDocument() {
		return document;
	}
}
