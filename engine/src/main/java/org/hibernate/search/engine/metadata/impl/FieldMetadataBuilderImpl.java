/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldMetadataCreationContext;
import org.hibernate.search.bridge.spi.FieldType;

/**
 * The internal implementation of {@link FieldMetadataBuilder}.
 *
 * @author Gunnar Morling
 */
class FieldMetadataBuilderImpl implements FieldMetadataBuilder {

	private final Set<String> sortableFields = new HashSet<>();
	private final Set<BridgeDefinedField> fields = new HashSet<>();
	private final DocumentFieldMetadata fieldMetadata;

	public FieldMetadataBuilderImpl(DocumentFieldMetadata fieldMetadata) {
		this.fieldMetadata = fieldMetadata;
	}

	@Override
	public FieldMetadataCreationContext field(String name, FieldType type) {
		return new FieldMetadataCreationContextImpl( name, type );
	}

	public Set<String> getSortableFields() {
		return sortableFields;
	}

	public Set<BridgeDefinedField> getFields() {
		return fields;
	}

	private class FieldMetadataCreationContextImpl implements FieldMetadataCreationContext {

		private final String fieldName;
		private DocumentFieldMetadata.Builder builder;

		public FieldMetadataCreationContextImpl(String name, FieldType type) {
			this.fieldName = name;
			fields.add( new BridgeDefinedField( name, type, fieldMetadata.getIndex() ) );
		}

		@Override
		public FieldMetadataCreationContext field(String name, FieldType type) {
			return FieldMetadataBuilderImpl.this.field( name, type );
		}

		@Override
		public FieldMetadataCreationContext sortable(boolean sortable) {
			sortableFields.add( fieldName );
			return this;
		}
	}
}
