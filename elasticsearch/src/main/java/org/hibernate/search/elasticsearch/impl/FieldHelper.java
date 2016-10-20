/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.regex.Pattern;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.builtin.NumericFieldBridge;
import org.hibernate.search.bridge.builtin.impl.NullEncodingTwoWayFieldBridge;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.engine.metadata.impl.BridgeDefinedField;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.PropertyMetadata;
import org.hibernate.search.engine.metadata.impl.SortableFieldMetadata;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;

/**
 * Helps with getting the property types for given fields.
 * <p>
 * Very hack-ish solution which is required atm. as we don't have access to the actual property types when dealing with
 * document fields in the work visitor. All this code should not be needed ideally.
 * <p>
 *
 * @author Gunnar Morling
 */
class FieldHelper {

	private static final Pattern DOT = Pattern.compile( "\\." );

	private FieldHelper() {
	}

	public enum ExtendedFieldType {
		STRING,
		BOOLEAN,
		DATE,
		CALENDAR,
		INSTANT,
		LOCAL_DATE,
		LOCAL_TIME,
		LOCAL_DATE_TIME,
		OFFSET_DATE_TIME,
		OFFSET_TIME,
		ZONED_DATE_TIME,
		YEAR,
		YEAR_MONTH,
		MONTH_DAY,
		INTEGER {
			public boolean isNumeric() {
				return true;
			}
		},
		LONG {
			public boolean isNumeric() {
				return true;
			}
		},
		FLOAT {
			public boolean isNumeric() {
				return true;
			}
		},
		DOUBLE {
			public boolean isNumeric() {
				return true;
			}
		},
		UNKNOWN_NUMERIC {
			public boolean isNumeric() {
				return true;
			}
		},
		UNKNOWN;

		public boolean isNumeric() {
			return false;
		}
	}

	// TODO HSEARCH-2259 make it work with fields embedded types
	private static NumericEncodingType getNumericEncodingType(DocumentFieldMetadata field) {
		NumericEncodingType numericEncodingType = field.getNumericEncodingType();

		if ( numericEncodingType == NumericEncodingType.UNKNOWN ) {
			PropertyMetadata hostingProperty = field.getSourceProperty();
			if ( hostingProperty != null ) {
				BridgeDefinedField bridgeDefinedField = hostingProperty.getBridgeDefinedFields().get( field.getName() );
				if ( bridgeDefinedField != null ) {
					numericEncodingType = getNumericEncodingType( bridgeDefinedField.getType() );
				}
			}
		}

		return numericEncodingType;
	}

	private static NumericEncodingType getNumericEncodingType(FieldType fieldType) {
		switch ( fieldType ) {
			case FLOAT:
				return NumericEncodingType.FLOAT;
			case DOUBLE:
				return NumericEncodingType.DOUBLE;
			case INTEGER:
				return NumericEncodingType.INTEGER;
			case LONG:
				return NumericEncodingType.LONG;
			default:
				return NumericEncodingType.UNKNOWN;
		}
	}

	private static ExtendedFieldType toExtendedFieldType(NumericEncodingType numericEncodingType) {
		switch ( numericEncodingType ) {
			case INTEGER:
				return ExtendedFieldType.INTEGER;
			case LONG:
				return ExtendedFieldType.LONG;
			case FLOAT:
				return ExtendedFieldType.FLOAT;
			case DOUBLE:
				return ExtendedFieldType.DOUBLE;
			case UNKNOWN:
			default:
				return ExtendedFieldType.UNKNOWN_NUMERIC;
		}
	}

	static ExtendedFieldType getType(DocumentFieldMetadata fieldMetadata) {
		PropertyMetadata propertyMetata = fieldMetadata.getSourceProperty();
		Class<?> propertyClass = propertyMetata == null ? null : propertyMetata.getPropertyClass();
		if ( propertyClass == null ) {
			return ExtendedFieldType.UNKNOWN;
		}

		if ( boolean.class.equals( propertyClass ) || Boolean.class.isAssignableFrom( propertyClass ) ) {
			return ExtendedFieldType.BOOLEAN;
		}
		else if ( isNumeric(fieldMetadata) ) {
			return toExtendedFieldType( getNumericEncodingType( fieldMetadata ) );
		}
		else if ( Date.class.isAssignableFrom( propertyClass ) ) {
			return ExtendedFieldType.DATE;
		}
		else if ( Calendar.class.isAssignableFrom( propertyClass ) ) {
			return ExtendedFieldType.CALENDAR;
		}
		// For the following, don't reference the class directly, in case we're in JDK 7
		// TODO HSEARCH-2350 Reference the class directly when JDK8 becomes mandatory
		else if ( "java.time.Instant".equals( propertyClass.getName() ) ) {
			return ExtendedFieldType.INSTANT;
		}
		else if ( "java.time.LocalDate".equals( propertyClass.getName() ) ) {
			return ExtendedFieldType.LOCAL_DATE;
		}
		else if ( "java.time.LocalTime".equals( propertyClass.getName() ) ) {
			return ExtendedFieldType.LOCAL_TIME;
		}
		else if ( "java.time.LocalDateTime".equals( propertyClass.getName() ) ) {
			return ExtendedFieldType.LOCAL_DATE_TIME;
		}
		else if ( "java.time.OffsetDateTime".equals( propertyClass.getName() ) ) {
			return ExtendedFieldType.OFFSET_DATE_TIME;
		}
		else if ( "java.time.OffsetTime".equals( propertyClass.getName() ) ) {
			return ExtendedFieldType.OFFSET_TIME;
		}
		else if ( "java.time.ZonedDateTime".equals( propertyClass.getName() ) ) {
			return ExtendedFieldType.ZONED_DATE_TIME;
		}
		else if ( "java.time.Year".equals( propertyClass.getName() ) ) {
			return ExtendedFieldType.YEAR;
		}
		else if ( "java.time.YearMonth".equals( propertyClass.getName() ) ) {
			return ExtendedFieldType.YEAR_MONTH;
		}
		else if ( "java.time.MonthDay".equals( propertyClass.getName() ) ) {
			return ExtendedFieldType.MONTH_DAY;
		}
		else {
			return ExtendedFieldType.UNKNOWN;
		}
	}

	static ExtendedFieldType getType(BridgeDefinedField field) {
		FieldType type = field.getType();
		if ( type == null ) {
			return null;
		}
		switch ( type ) {
			case BOOLEAN:
				return ExtendedFieldType.BOOLEAN;
			case DATE:
				return ExtendedFieldType.DATE;
			case DOUBLE:
				return ExtendedFieldType.DOUBLE;
			case FLOAT:
				return ExtendedFieldType.FLOAT;
			case INTEGER:
				return ExtendedFieldType.INTEGER;
			case LONG:
				return ExtendedFieldType.LONG;
			case STRING:
				return ExtendedFieldType.STRING;
			default:
				return ExtendedFieldType.UNKNOWN;
		}
	}

	static boolean isNumeric(DocumentFieldMetadata field) {
		if ( field.isNumeric() ) {
			return true;
		}

		FieldBridge fieldBridge = field.getFieldBridge();

		if ( fieldBridge instanceof NullEncodingTwoWayFieldBridge ) {
			return ( (NullEncodingTwoWayFieldBridge) fieldBridge ).unwrap() instanceof NumericFieldBridge;
		}

		return false;
	}

	static String[] getFieldNameParts(String fieldName) {
		boolean isEmbeddedField = isEmbeddedField( fieldName );
		return isEmbeddedField ? DOT.split( fieldName ) : new String[]{ fieldName };
	}

	static DocumentFieldMetadata getFieldMetadata(EntityIndexBinding indexBinding, String fieldName) {
		// This also addresses the ID case
		DocumentFieldMetadata result = indexBinding.getDocumentBuilder().getMetadata().getDocumentFieldMetadataFor( fieldName );
		if ( result != null ) {
			return result;
		}

		Set<DocumentFieldMetadata> classBridgeMetadata = indexBinding.getDocumentBuilder().getMetadata().getClassBridgeMetadata();
		for ( DocumentFieldMetadata documentFieldMetadata : classBridgeMetadata ) {
			if ( documentFieldMetadata.getFieldName().equals( fieldName ) ) {
				return documentFieldMetadata;
			}
		}

		return null;
	}

	public static boolean isSortableField(PropertyMetadata sourceProperty, String fieldName) {
		for ( SortableFieldMetadata sortableField : sourceProperty.getSortableFieldMetadata() ) {
			if ( fieldName.equals( sortableField.getFieldName() ) ) {
				return true;
			}
		}
		return false;
	}

	public static boolean isEmbeddedField(String field) {
		return field.contains( "." );
	}

	public static String getEmbeddedFieldPath(String field) {
		return field.substring( 0, field.lastIndexOf( "." ) );
	}

	public static String getEmbeddedFieldPropertyName(String field) {
		return field.substring( field.lastIndexOf( "." ) + 1 );
	}
}
