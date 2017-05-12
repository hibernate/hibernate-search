/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.regex.Pattern;

import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.engine.metadata.impl.BridgeDefinedField;
import org.hibernate.search.engine.metadata.impl.PartialDocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.PartialPropertyMetadata;
import org.hibernate.search.engine.metadata.impl.PropertyMetadata;
import org.hibernate.search.engine.metadata.impl.SortableFieldMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
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
public class FieldHelper {

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
		OBJECT,
		INTEGER {
			@Override
			public boolean isNumeric() {
				return true;
			}
		},
		LONG {
			@Override
			public boolean isNumeric() {
				return true;
			}
		},
		FLOAT {
			@Override
			public boolean isNumeric() {
				return true;
			}
		},
		DOUBLE {
			@Override
			public boolean isNumeric() {
				return true;
			}
		},
		UNKNOWN_NUMERIC {
			@Override
			public boolean isNumeric() {
				return true;
			}
		},
		UNKNOWN;

		public boolean isNumeric() {
			return false;
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

	public static ExtendedFieldType getType(PartialDocumentFieldMetadata fieldMetadata) {
		// Always use user-provided type in priority
		BridgeDefinedField overriddenField = fieldMetadata.getBridgeDefinedFields().get( fieldMetadata.getPath().getAbsoluteName() );
		if ( overriddenField != null ) {
			return getType( overriddenField );
		}

		PartialPropertyMetadata propertyMetata = fieldMetadata.getSourceProperty();
		Class<?> propertyClass = propertyMetata == null ? null : propertyMetata.getPropertyClass();
		if ( propertyClass == null ) {
			return ExtendedFieldType.UNKNOWN;
		}

		if ( fieldMetadata.isNumeric() ) {
			return toExtendedFieldType( fieldMetadata.getNumericEncodingType() );
		}
		else {
			return getType( propertyClass );
		}
	}

	public static ExtendedFieldType getType(Class<?> propertyClass) {
		if ( boolean.class.equals( propertyClass ) || Boolean.class.isAssignableFrom( propertyClass ) ) {
			return ExtendedFieldType.BOOLEAN;
		}
		else if ( Date.class.isAssignableFrom( propertyClass ) ) {
			return ExtendedFieldType.DATE;
		}
		else if ( Calendar.class.isAssignableFrom( propertyClass ) ) {
			return ExtendedFieldType.CALENDAR;
		}
		// For the following, don't reference the class directly, in case we're in JDK 7
		else if ( Instant.class.equals( propertyClass ) ) {
			return ExtendedFieldType.INSTANT;
		}
		else if ( LocalDate.class.equals( propertyClass ) ) {
			return ExtendedFieldType.LOCAL_DATE;
		}
		else if ( LocalTime.class.equals( propertyClass ) ) {
			return ExtendedFieldType.LOCAL_TIME;
		}
		else if ( LocalDateTime.class.equals( propertyClass ) ) {
			return ExtendedFieldType.LOCAL_DATE_TIME;
		}
		else if ( OffsetDateTime.class.equals( propertyClass ) ) {
			return ExtendedFieldType.OFFSET_DATE_TIME;
		}
		else if ( OffsetTime.class.equals( propertyClass ) ) {
			return ExtendedFieldType.OFFSET_TIME;
		}
		else if ( ZonedDateTime.class.equals( propertyClass ) ) {
			return ExtendedFieldType.ZONED_DATE_TIME;
		}
		else if ( Year.class.equals( propertyClass ) ) {
			return ExtendedFieldType.YEAR;
		}
		else if ( YearMonth.class.equals( propertyClass ) ) {
			return ExtendedFieldType.YEAR_MONTH;
		}
		else if ( MonthDay.class.equals( propertyClass ) ) {
			return ExtendedFieldType.MONTH_DAY;
		}
		else {
			return ExtendedFieldType.UNKNOWN;
		}
	}

	public static ExtendedFieldType getType(BridgeDefinedField field) {
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
			case OBJECT:
				return ExtendedFieldType.OBJECT;
			default:
				return ExtendedFieldType.UNKNOWN;
		}
	}

	public static String[] getFieldNameParts(String fieldName) {
		boolean isEmbeddedField = isEmbeddedField( fieldName );
		return isEmbeddedField ? DOT.split( fieldName ) : new String[]{ fieldName };
	}

	public static boolean isSortableField(TypeMetadata sourceType, PropertyMetadata sourceProperty, String fieldName) {
		Collection<SortableFieldMetadata> sortableFields = sourceProperty != null ? sourceProperty.getSortableFieldMetadata()
				: sourceType.getClassBridgeSortableFieldMetadata();
		for ( SortableFieldMetadata sortableField : sortableFields ) {
			if ( fieldName.equals( sortableField.getAbsoluteName() ) ) {
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
