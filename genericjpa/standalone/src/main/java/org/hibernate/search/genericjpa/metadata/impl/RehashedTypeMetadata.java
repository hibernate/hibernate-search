/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.metadata.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.backend.spi.SingularTermDeletionQuery;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;

/**
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
	Map<Class<?>, List<String>> idFieldNamesForType = new HashMap<>();

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
	Map<String, DocumentFieldMetadata> documentFieldMetadataForIdFieldName = new HashMap<>();

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
	public Map<Class<?>, List<String>> getIdFieldNamesForType() {
		return idFieldNamesForType;
	}

	/**
	 * @param idFieldNamesForType the idFieldNamesForType to set
	 */
	public void setIdFieldNamesForType(Map<Class<?>, List<String>> idFieldNamesForType) {
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
	 * @return the documentFieldMetadataForIdFieldName
	 */
	public Map<String, DocumentFieldMetadata> getDocumentFieldMetadataForIdFieldName() {
		return documentFieldMetadataForIdFieldName;
	}

	/**
	 * @param documentFieldMetadataForIdFieldName the documentFieldMetadataForIdFieldName to set
	 */
	public void setDocumentFieldMetadataForIdFieldName(Map<String, DocumentFieldMetadata> documentFieldMetadataForIdFieldName) {
		this.documentFieldMetadataForIdFieldName = documentFieldMetadataForIdFieldName;
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
