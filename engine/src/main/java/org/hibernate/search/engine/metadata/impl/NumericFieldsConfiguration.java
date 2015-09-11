/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.NumericFields;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Represents the configuration of numeric fields of a specific entity property.
 *
 * @author Gunnar Morling
 */
class NumericFieldsConfiguration {

	private static final Log LOG = LoggerFactory.make();

	private final Class<?> indexedType;
	private final XProperty member;
	private final Map<String, NumericField> fieldsMarkedAsNumeric;
	private final Set<String> fieldsOfProperty = new HashSet<>();

	NumericFieldsConfiguration(Class<?> indexedType, XProperty member) {
		this.indexedType = indexedType;
		this.member = member;
		fieldsMarkedAsNumeric = new HashMap<>();

		NumericField numericFieldAnnotation = member.getAnnotation( NumericField.class );
		if ( numericFieldAnnotation != null ) {
			fieldsMarkedAsNumeric.put( numericFieldAnnotation.forField(), numericFieldAnnotation );
		}

		NumericFields numericFieldsAnnotation = member.getAnnotation( NumericFields.class );
		if ( numericFieldsAnnotation != null ) {
			for ( NumericField numericField : numericFieldsAnnotation.value() ) {
				NumericField existing = fieldsMarkedAsNumeric.put( numericField.forField(), numericField );
				if ( existing != null ) {
					throw LOG.severalNumericFieldAnnotationsForSameField( indexedType, member.getName() );
				}
			}
		}
	}

	/**
	 * Gets the {@code @NumericField} annotation matching the given field, if any. As a side-effect, the given field
	 * is collected for later validation of configured numeric fields against actually present fields.
	 */
	NumericField getNumericFieldAnnotation(String fieldName) {
		fieldName = unqualify( fieldName );

		fieldsOfProperty.add( fieldName );

		NumericField numericFieldAnnotation = fieldsMarkedAsNumeric.get( fieldName );

		if ( numericFieldAnnotation == null ) {
			numericFieldAnnotation = fieldsMarkedAsNumeric.get( "" );
		}

		return numericFieldAnnotation;
	}

	boolean isNumericField(String fieldName) {
		return getNumericFieldAnnotation( fieldName ) != null;
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

			throw LOG.numericFieldAnnotationWithoutMatchingField( indexedType, member.getName() );
		}
	}

	private String unqualify(String fieldName) {
		int separatorIdx = fieldName.lastIndexOf( '.' );
		if ( separatorIdx != -1 ) {
			fieldName = fieldName.substring( separatorIdx + 1 );
		}

		return fieldName;
	}
}