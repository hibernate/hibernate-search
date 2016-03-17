/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.metadata.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.backend.spi.SingularTermDeletionQuery;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;

/**
 * This class represents a "rehashed" version of the
 * TypeMetadata the engine gives us. These are mostly needed
 * for the async backend to know where to find the id fields
 * (on the class and in the index so the index can be held up-to-date
 * properly) and the types of the fields
 *
 * @author Martin Braun
 */
public final class RehashedTypeMetadata {

	/**
	 * the original TypeMetadata object this object rehashes
	 */
	TypeMetadata originalTypeMetadata;

	/**
	 * this contains all the possible fields in the lucene index for every given class contained in the index this
	 * metadata object corresponds to
	 */
	Map<Class<?>, Set<String>> idFieldNamesForType = new HashMap<>();

	/**
	 * this contains the Java Bean property name of the id field for every given class contained in the index. every
	 * propertyname is relative to it's hosting entity instead of the index-root. This is needed to be able to retrieve
	 * the entity from the database
	 */
	Map<Class<?>, String> idPropertyNameForType = new HashMap<>();

	Map<Class<?>, XProperty> idPropertyAccessorForType = new HashMap<>();

	/**
	 * this contains the DocumentFieldMetadata for each id-fieldname. This provides info about how each id is stored in
	 * the index
	 */
	Map<String, FieldBridge> fieldBridgeForIdFieldName = new HashMap<>();

	Map<String, SingularTermDeletionQuery.Type> singularTermDeletionQueryTypeForIdFieldName = new HashMap<>();

	/**
	 * @return the originalTypeMetadata
	 */
	public TypeMetadata getOriginalTypeMetadata() {
		return originalTypeMetadata;
	}

	/**
	 * @param originalTypeMetadata the originalTypeMetadata to set
	 */
	public void setOriginalTypeMetadata(TypeMetadata originalTypeMetadata) {
		this.originalTypeMetadata = originalTypeMetadata;
	}

	/**
	 * @return the idFieldNamesForType
	 */
	public Map<Class<?>, Set<String>> getIdFieldNamesForType() {
		return idFieldNamesForType;
	}

	/**
	 * @param idFieldNamesForType the idFieldNamesForType to set
	 */
	public void setIdFieldNamesForType(Map<Class<?>, Set<String>> idFieldNamesForType) {
		this.idFieldNamesForType = idFieldNamesForType;
	}

	/**
	 * @return the idPropertyNameForType
	 */
	public Map<Class<?>, String> getIdPropertyNameForType() {
		return idPropertyNameForType;
	}

	/**
	 * @param idPropertyNameForType the idPropertyNameForType to set
	 */
	public void setIdPropertyNameForType(Map<Class<?>, String> idPropertyNameForType) {
		this.idPropertyNameForType = idPropertyNameForType;
	}

	/**
	 * @return the fieldBridgeForIdFieldName
	 */
	public Map<String, FieldBridge> getFieldBridgeForIdFieldName() {
		return fieldBridgeForIdFieldName;
	}

	/**
	 * @param fieldBridgeForIdFieldName the fieldBridgeForIdFieldName to set
	 */
	public void setFieldBridgeForIdFieldName(Map<String, FieldBridge> fieldBridgeForIdFieldName) {
		this.fieldBridgeForIdFieldName = fieldBridgeForIdFieldName;
	}

	/**
	 * @return the singularTermDeletionQueryTypeForIdFieldName
	 */
	public Map<String, SingularTermDeletionQuery.Type> getSingularTermDeletionQueryTypeForIdFieldName() {
		return singularTermDeletionQueryTypeForIdFieldName;
	}

	/**
	 * @param singularTermDeletionQueryTypeForIdFieldName the singularTermDeletionQueryTypeForIdFieldName to set
	 */
	public void setSingularTermDeletionQueryTypeForIdFieldName(Map<String, SingularTermDeletionQuery.Type> singularTermDeletionQueryTypeForIdFieldName) {
		this.singularTermDeletionQueryTypeForIdFieldName = singularTermDeletionQueryTypeForIdFieldName;
	}

	public Map<Class<?>, XProperty> getIdPropertyAccessorForType() {
		return idPropertyAccessorForType;
	}

	public void setIdPropertyAccessorForType(Map<Class<?>, XProperty> idPropertyAccessorForType) {
		this.idPropertyAccessorForType = idPropertyAccessorForType;
	}
}
