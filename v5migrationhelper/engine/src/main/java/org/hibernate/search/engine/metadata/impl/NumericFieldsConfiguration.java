/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * Represents the configuration of numeric fields of a specific entity property.
 *
 * @author Gunnar Morling
 */
class NumericFieldsConfiguration {

	private static final Log LOG = LoggerFactory.make( MethodHandles.lookup() );

	private final IndexedTypeIdentifier indexedType;
	private final String propertyName;
	private final Map<String, NumericField> fieldsMarkedAsNumeric;
	private final Set<String> fieldsOfProperty = new LinkedHashSet<>();

	NumericFieldsConfiguration(IndexedTypeIdentifier indexedTypeIdentifier, String propertyName, Map<String, NumericField> fieldsMarkedAsNumeric) {
		this.indexedType = indexedTypeIdentifier;
		this.propertyName = propertyName;
		this.fieldsMarkedAsNumeric = fieldsMarkedAsNumeric;
	}

	/**
	 * Gets the {@code @NumericField} annotation matching the given field, if any. As a side-effect, the given field is
	 * collected for later validation of configured numeric fields against actually present fields.
	 *
	 * @param unprefixedFieldName The name of the field of interest, without any prefixes it may inherited from the
	 * parent in case it's part of an embedded entity
	 */
	NumericField getNumericFieldAnnotation(final DocumentFieldPath path) {
		fieldsOfProperty.add( path.getRelativeName() );

		NumericField numericFieldAnnotation = fieldsMarkedAsNumeric.get( path.getRelativeName() );

		if ( numericFieldAnnotation == null ) {
			numericFieldAnnotation = fieldsMarkedAsNumeric.get( "" );
		}

		return numericFieldAnnotation;
	}

	boolean isNumericField(DocumentFieldPath path) {
		return getNumericFieldAnnotation( path ) != null;
	}

	/**
	 * Validates correctness of the {@link NumericField} annotations declared at the given member.
	 */
	void validate() {
		for ( String fieldMarkedAsNumeric : fieldsMarkedAsNumeric.keySet() ) {
			// valid reference or reference with default name and there is a field
			if ( fieldsOfProperty.contains( fieldMarkedAsNumeric ) ||
					( fieldMarkedAsNumeric.isEmpty() && !fieldsOfProperty.isEmpty() ) ) {
				continue;
			}

			throw LOG.numericFieldAnnotationWithoutMatchingField( indexedType, propertyName );
		}
	}

}