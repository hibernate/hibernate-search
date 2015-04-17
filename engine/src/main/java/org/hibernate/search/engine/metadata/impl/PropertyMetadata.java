/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.engine.impl.DefaultBoostStrategy;
import org.hibernate.search.util.impl.ReflectionHelper;

/**
 * Encapsulating the metadata for a single indexed property (field or getter).
 *
 * Each field or getter can have multiple document fields (via {@code @Fields}).
 *
 * @author Hardy Ferentschik
 */
public class PropertyMetadata {
	private final XProperty propertyAccessor;
	private final Map<String, DocumentFieldMetadata> documentFieldMetadataMap;
	private final Set<DocumentFieldMetadata> documentFieldMetadataList;
	private final BoostStrategy dynamicBoostStrategy;
	private final String propertyAccessorName;

	private PropertyMetadata(Builder builder) {
		this.propertyAccessor = builder.propertyAccessor;
		this.documentFieldMetadataList = Collections.unmodifiableSet( builder.fieldMetadataSet );
		this.documentFieldMetadataMap = createDocumentFieldMetadataMap( builder.fieldMetadataSet );
		this.propertyAccessorName = propertyAccessor == null ? null : propertyAccessor.getName();
		if ( builder.dynamicBoostStrategy != null ) {
			this.dynamicBoostStrategy = builder.dynamicBoostStrategy;
		}
		else {
			this.dynamicBoostStrategy = DefaultBoostStrategy.INSTANCE;
		}
	}

	private Map<String, DocumentFieldMetadata> createDocumentFieldMetadataMap(Set<DocumentFieldMetadata> fieldMetadataSet) {
		Map<String, DocumentFieldMetadata> tmpMap = new HashMap<>();
		for ( DocumentFieldMetadata documentFieldMetadata : fieldMetadataSet ) {
			tmpMap.put( documentFieldMetadata.getName(), documentFieldMetadata );
		}
		return Collections.unmodifiableMap( tmpMap );
	}

	public XProperty getPropertyAccessor() {
		return propertyAccessor;
	}

	public String getPropertyAccessorName() {
		return propertyAccessorName;
	}

	public Set<DocumentFieldMetadata> getFieldMetadata() {
		return documentFieldMetadataList;
	}

	public DocumentFieldMetadata getFieldMetadata(String fieldName) {
		return documentFieldMetadataMap.get( fieldName );
	}

	public BoostStrategy getDynamicBoostStrategy() {
		return dynamicBoostStrategy;
	}

	public static class Builder {
		// required parameters
		private final XProperty propertyAccessor;
		private final Set<DocumentFieldMetadata> fieldMetadataSet;

		// optional parameters
		private BoostStrategy dynamicBoostStrategy;

		public Builder(XProperty propertyAccessor) {
			if ( propertyAccessor != null ) {
				ReflectionHelper.setAccessible( propertyAccessor );
			}
			this.propertyAccessor = propertyAccessor;
			this.fieldMetadataSet = new HashSet<>();
		}

		public Builder dynamicBoostStrategy(BoostStrategy boostStrategy) {
			this.dynamicBoostStrategy = boostStrategy;
			return this;
		}

		public Builder addDocumentField(DocumentFieldMetadata documentFieldMetadata) {
			this.fieldMetadataSet.add( documentFieldMetadata );
			return this;
		}

		public XProperty getPropertyAccessor() {
			return propertyAccessor;
		}

		public PropertyMetadata build() {
			return new PropertyMetadata( this );
		}
	}

	@Override
	public String toString() {
		return "PropertyMetadata{"
				+ "propertyAccessor=" + propertyAccessor
				+ ", fieldMetadata=" + documentFieldMetadataList
				+ ", dynamicBoostStrategy=" + dynamicBoostStrategy + '}';
	}
}


