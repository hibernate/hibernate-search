/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.engine.metadata.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.engine.impl.DefaultBoostStrategy;
import org.hibernate.search.util.impl.ReflectionHelper;

import static org.hibernate.search.util.impl.CollectionHelper.toImmutableList;

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
	private final List<DocumentFieldMetadata> documentFieldMetadataList;
	private final BoostStrategy dynamicBoostStrategy;
	private final String propertyAccessorName;

	private PropertyMetadata(Builder builder) {
		this.propertyAccessor = builder.propertyAccessor;
		this.documentFieldMetadataList = toImmutableList( builder.fieldMetadataSet );
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
		Map<String, DocumentFieldMetadata> tmpMap = new HashMap<String, DocumentFieldMetadata>();
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

	public List<DocumentFieldMetadata> getFieldMetadata() {
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
			this.fieldMetadataSet = new HashSet<DocumentFieldMetadata>();
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
		final StringBuilder sb = new StringBuilder( "PropertyMetadata{" );
		sb.append( "propertyAccessor=" ).append( propertyAccessor );
		sb.append( ", fieldMetadata=" ).append( documentFieldMetadataList );
		sb.append( ", dynamicBoostStrategy=" ).append( dynamicBoostStrategy );
		sb.append( '}' );
		return sb.toString();
	}
}


