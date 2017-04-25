/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.engine.impl.DefaultBoostStrategy;

/**
 * Encapsulating the metadata for a single indexed property (field or getter).
 *
 * Each field or getter can have multiple document fields (via {@code @Fields}).
 *
 * @author Hardy Ferentschik
 */
public class PropertyMetadata implements PartialPropertyMetadata {
	private final BackReference<TypeMetadata> declaringType;
	private final XProperty propertyAccessor;
	private final Class<?> propertyClass;
	private final Map<String, DocumentFieldMetadata> documentFieldMetadataMap;
	private final Set<DocumentFieldMetadata> documentFieldMetadataList;
	private final Set<SortableFieldMetadata> sortableFieldMetadata;
	private final BoostStrategy dynamicBoostStrategy;
	private final String propertyAccessorName;

	private PropertyMetadata(Builder builder) {
		this.declaringType = builder.declaringType;
		this.propertyAccessor = builder.propertyAccessor;
		this.propertyClass = builder.propertyClass;
		this.documentFieldMetadataList = Collections.unmodifiableSet( builder.fieldMetadataSet );
		this.documentFieldMetadataMap = createDocumentFieldMetadataMap( builder.fieldMetadataSet );
		this.sortableFieldMetadata = Collections.unmodifiableSet( builder.sortableFieldMetadata );
		this.propertyAccessorName = propertyAccessor == null ? null : propertyAccessor.getName();
		if ( builder.dynamicBoostStrategy != null ) {
			this.dynamicBoostStrategy = builder.dynamicBoostStrategy;
		}
		else {
			this.dynamicBoostStrategy = DefaultBoostStrategy.INSTANCE;
		}
	}

	private Map<String, DocumentFieldMetadata> createDocumentFieldMetadataMap(Set<DocumentFieldMetadata> fieldMetadataSet) {
		Map<String, DocumentFieldMetadata> tmpMap = new LinkedHashMap<>();
		for ( DocumentFieldMetadata documentFieldMetadata : fieldMetadataSet ) {
			tmpMap.put( documentFieldMetadata.getAbsoluteName(), documentFieldMetadata );
		}
		return Collections.unmodifiableMap( tmpMap );
	}


	/**
	 * @return The type declaring this property.
	 */
	public TypeMetadata getDeclaringType() {
		return declaringType.get();
	}

	public XProperty getPropertyAccessor() {
		return propertyAccessor;
	}

	@Override
	public Class<?> getPropertyClass() {
		return propertyClass;
	}

	public String getPropertyAccessorName() {
		return propertyAccessorName;
	}

	/**
	 * @return A newly created list containing this property's field metadata.
	 * @deprecated use getFieldMetadataSet() instead
	 */
	@Deprecated
	public List<DocumentFieldMetadata> getFieldMetadata() {
		return new ArrayList<DocumentFieldMetadata>( documentFieldMetadataList );
	}

	public Set<DocumentFieldMetadata> getFieldMetadataSet() {
		return documentFieldMetadataList;
	}

	public DocumentFieldMetadata getFieldMetadata(String fieldName) {
		return documentFieldMetadataMap.get( fieldName );
	}

	/**
	 * Accessor to the metadata of the sortable fields.
	 *
	 * @return the sortable fields defined for this property.
	 */
	public Set<SortableFieldMetadata> getSortableFieldMetadata() {
		return sortableFieldMetadata;
	}

	public BoostStrategy getDynamicBoostStrategy() {
		return dynamicBoostStrategy;
	}

	public static class Builder implements PartialPropertyMetadata {
		private final BackReference<PropertyMetadata> resultReference = new BackReference<>();

		// required parameters
		private final BackReference<TypeMetadata> declaringType;
		private final XProperty propertyAccessor;
		private final Class<?> propertyClass;
		private final Set<DocumentFieldMetadata> fieldMetadataSet;
		private final Set<SortableFieldMetadata> sortableFieldMetadata;

		// optional parameters
		private BoostStrategy dynamicBoostStrategy;

		public Builder(BackReference<TypeMetadata> declaringType, XProperty propertyAccessor, Class<?> propertyClass) {
			this.declaringType = declaringType;
			this.propertyAccessor = propertyAccessor;
			this.propertyClass = propertyClass;
			this.fieldMetadataSet = new LinkedHashSet<>();
			this.sortableFieldMetadata = new LinkedHashSet<>();
		}

		@Override
		public Class<?> getPropertyClass() {
			return propertyClass;
		}

		public Builder dynamicBoostStrategy(BoostStrategy boostStrategy) {
			this.dynamicBoostStrategy = boostStrategy;
			return this;
		}

		public Builder addDocumentField(DocumentFieldMetadata documentFieldMetadata) {
			this.fieldMetadataSet.add( documentFieldMetadata );
			return this;
		}

		public Builder addSortableField(SortableFieldMetadata sortableField) {
			this.sortableFieldMetadata.add( sortableField );
			return this;
		}

		public XProperty getPropertyAccessor() {
			return propertyAccessor;
		}

		public Set<DocumentFieldMetadata> getFieldMetadata() {
			return fieldMetadataSet;
		}


		public BackReference<PropertyMetadata> getResultReference() {
			return resultReference;
		}

		public PropertyMetadata build() {
			PropertyMetadata result = new PropertyMetadata( this );
			resultReference.initialize( result );
			return result;
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
