/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

import java.util.LinkedHashSet;
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

	private final Set<String> sortableFieldsAbsoluteNames = new LinkedHashSet<>();
	private final Set<BridgeDefinedField> bridgeDefinedFields = new LinkedHashSet<>();
	private final BackReference<DocumentFieldMetadata> fieldMetadata;

	public FieldMetadataBuilderImpl(BackReference<DocumentFieldMetadata> fieldMetadata) {
		this.fieldMetadata = fieldMetadata;
	}

	@Override
	public FieldMetadataCreationContext field(String name, FieldType type) {
		return new FieldMetadataCreationContextImpl( name, type );
	}

	public Set<String> getSortableFieldsAbsoluteNames() {
		return sortableFieldsAbsoluteNames;
	}

	public Set<BridgeDefinedField> getBridgeDefinedFields() {
		return bridgeDefinedFields;
	}

	private class FieldMetadataCreationContextImpl implements FieldMetadataCreationContext {

		private final String absoluteFieldName;

		public FieldMetadataCreationContextImpl(String name, FieldType type) {
			this.absoluteFieldName = name;
			bridgeDefinedFields.add( new BridgeDefinedField( fieldMetadata, name, type ) );
		}

		@Override
		public FieldMetadataCreationContext field(String name, FieldType type) {
			return FieldMetadataBuilderImpl.this.field( name, type );
		}

		@Override
		public FieldMetadataCreationContext sortable(boolean sortable) {
			sortableFieldsAbsoluteNames.add( absoluteFieldName );
			return this;
		}
	}
}
