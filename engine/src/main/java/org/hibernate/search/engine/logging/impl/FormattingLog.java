/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.logging.impl;

import static org.hibernate.search.engine.logging.impl.EngineLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.logging.impl.SimpleNameClassFormatter;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = FormattingLog.CATEGORY_NAME,
		description = "Logs related to parsing/formatting."
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface FormattingLog {
	String CATEGORY_NAME = "org.hibernate.search.formatting";

	FormattingLog INSTANCE = LoggerFactory.make( FormattingLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	@Message(id = ID_OFFSET + 2,
			value = "Invalid value: expected either an instance of '%1$s' or a String that can be parsed into that type. %2$s")
	SearchException invalidPropertyValue(@FormatWith(ClassFormatter.class) Class<?> expectedType, String errorMessage,
			@Cause Exception cause);

	@Message(id = ID_OFFSET + 3,
			value = "Invalid Boolean value: expected either a Boolean, the String 'true' or the String 'false'. %1$s")
	SearchException invalidBooleanPropertyValue(String nestedErrorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 4,
			value = "Invalid Integer value: expected either a Number or a String that can be parsed into an Integer. %1$s")
	SearchException invalidIntegerPropertyValue(String nestedErrorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 5,
			value = "Invalid Long value: expected either a Number or a String that can be parsed into a Long. %1$s")
	SearchException invalidLongPropertyValue(String nestedErrorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 6,
			value = "Invalid multi value: expected either a single value of the correct type, a Collection, or a String,"
					+ " and interpreting as a single value failed with the following exception. %1$s")
	SearchException invalidMultiPropertyValue(String nestedErrorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 16,
			value = "Invalid polygon: the first point '%1$s' should be identical to the last point '%2$s' to properly close the polygon.")
	IllegalArgumentException invalidGeoPolygonFirstPointNotIdenticalToLastPoint(GeoPoint firstPoint,
			GeoPoint lastPoint);

	@Message(id = ID_OFFSET + 47,
			value = "Invalid BeanReference value: expected an instance of '%1$s', BeanReference, String or Class. %2$s")
	SearchException invalidBeanReferencePropertyValue(@FormatWith(ClassFormatter.class) Class<?> expectedType,
			String nestedErrorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 57, value = "Invalid value for type '%1$s': '%2$s'."
			+ " The expected format is '%3$s'.")
	SearchException unableToParseTemporal(
			@FormatWith(SimpleNameClassFormatter.class) Class<? extends TemporalAccessor> type, String value,
			DateTimeFormatter formatter, @Cause Exception cause);

	@Message(id = ID_OFFSET + 58,
			value = "Invalid %1$s value: expected either a Number or a String that can be parsed into a %1$s. %2$s")
	SearchException invalidNumberPropertyValue(@FormatWith(SimpleNameClassFormatter.class) Class<? extends Number> type,
			String nestedErrorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 59, value = "Invalid string for type '%2$s': '%1$s'. %3$s")
	SearchException invalidStringForType(String value, @FormatWith(ClassFormatter.class) Class<?> type,
			String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 60, value = "Invalid value for enum '%2$s': '%1$s'.")
	SearchException invalidStringForEnum(String value,
			@FormatWith(ClassFormatter.class) Class<? extends Enum<?>> enumType, @Cause Exception cause);

	@Message(id = ID_OFFSET + 64,
			value = "Invalid geo-point value: '%1$s'."
					+ " The expected format is '<latitude as double>, <longitude as double>'.")
	SearchException unableToParseGeoPoint(String value);

}
