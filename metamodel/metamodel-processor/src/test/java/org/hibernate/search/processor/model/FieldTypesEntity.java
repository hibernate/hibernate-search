/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.model;

import java.math.BigDecimal;
import java.math.BigInteger;
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

import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScaledNumberField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.VectorField;

@Indexed
public class FieldTypesEntity {
	@DocumentId
	public String id;
	@FullTextField
	public String text;
	@KeywordField
	public String keywordString;
	@ScaledNumberField
	public BigDecimal bigDecimal;
	@ScaledNumberField
	public BigInteger bigInteger;
	@GenericField
	public boolean bool;
	@GenericField
	public Boolean boool;
	@GenericField
	public byte aByte;
	@GenericField
	public double aDouble;
	@GenericField
	public float aFloat;
	@GenericField
	public int aInt;
	@GenericField
	public long aLong;
	@GenericField
	public short aShort;
	@GenericField
	public GeoPoint geoPoint;
	@GenericField
	public Instant instant;
	@GenericField
	public LocalDate localDate;
	@GenericField
	public LocalDateTime localDateTime;
	@GenericField
	public LocalTime localTime;
	@GenericField
	public MonthDay monthDay;
	@GenericField
	public OffsetDateTime offsetDateTime;
	@GenericField
	public OffsetTime offsetTime;
	@GenericField
	public Year year;
	@GenericField
	public YearMonth yearMonth;
	@GenericField
	public ZonedDateTime zonedDateTime;

	@VectorField(dimension = 128)
	public float[] floatVector;
	@VectorField(dimension = 128)
	public byte[] byteVector;

	@KeywordField
	public MyEnum myEnum;
}
