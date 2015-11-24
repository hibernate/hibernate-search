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

	@Override
	public FieldMetadataCreationContext field(String name, FieldType type) {
		return new FieldMetadataCreationContextImpl( name );
	}

	public Set<String> getSortableFields() {
		return sortableFields;
	}

	private class FieldMetadataCreationContextImpl implements FieldMetadataCreationContext {

		private final String fieldName;

		public FieldMetadataCreationContextImpl(String name) {
			this.fieldName = name;
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
