/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import java.math.BigDecimal;
import java.math.BigInteger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScaledNumberField;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class DefaultDecimalScaleMappingIT {

	private static final String INDEX_NAME = "IndexName";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Test
	void mapping() {
		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "scaled", BigDecimal.class, f -> f.defaultDecimalScale( 7 ) )
				.field( "unscaled", BigDecimal.class, f -> f.defaultDecimalScale( 0 ) )
				.field( "defaultScaled", BigDecimal.class, f -> f.defaultDecimalScale( 2 ) )
				.field( "hsearch", BigDecimal.class, f -> f.defaultDecimalScale( 2 ).decimalScale( 7 ) )
				.field( "notDecimal", Integer.class )
				.field( "general", BigDecimal.class, f -> f.defaultDecimalScale( 3 ) )
				.field( "both", BigDecimal.class, f -> f.defaultDecimalScale( 3 ).decimalScale( 2 ) )
				.field( "generalInteger", BigInteger.class, f -> f.defaultDecimalScale( -3 ) )
				.field( "bothInteger", BigInteger.class, f -> f.defaultDecimalScale( -3 ).decimalScale( -2 ) )
		);

		ormSetupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void bigIntegerIdMapping() {
		backendMock.expectSchema( IdWithScale.NAME, b -> b
				.field( "id", BigInteger.class, f -> f.defaultDecimalScale( 0 ) )
				.field( "other", BigInteger.class, f -> f.defaultDecimalScale( 0 ) )
				.field( "decimalImplicit", BigDecimal.class, f -> f.defaultDecimalScale( 2 ) )
		);

		ormSetupHelper.start().setup( IdWithScale.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void bigDecimalIdMapping() {
		backendMock.expectSchema( DecimalIdWithScale.NAME, b -> b
				.field( "id", BigDecimal.class, f -> f.defaultDecimalScale( 2 ) )
		);

		ormSetupHelper.start().setup( DecimalIdWithScale.class );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = "indexed")
	@Indexed(index = INDEX_NAME)
	public static final class IndexedEntity {

		@Id
		@DocumentId
		private Integer id;

		@ScaledNumberField
		@Column(precision = 14, scale = 7)
		private BigDecimal scaled;

		@ScaledNumberField
		@Column(precision = 14)
		// If the precision attribute is set,
		// the default column scale is 0.
		private BigDecimal unscaled;

		@ScaledNumberField
		// Without a precision,
		// the default column scale will be taken from the underlying ORM mapping.
		// In the case of this test is 2.
		private BigDecimal defaultScaled;

		@ScaledNumberField(decimalScale = 7)
		private BigDecimal hsearch;

		@GenericField
		// The decimal scale value provided by Hibernate ORM is not used
		// if the field is not a decimal-number field.
		@Column(precision = 14, scale = 3)
		private Integer notDecimal;

		@GenericField
		// The decimal scale value provided by Hibernate ORM is used
		// even if we use a @GenericField to annotate the field
		@Column(precision = 14, scale = 3)
		private BigDecimal general;

		@ScaledNumberField(decimalScale = 2)
		// Only use two digits after the dot in the full-text index
		@Column(precision = 14, scale = 3)
		private BigDecimal both;

		@GenericField
		@Column(precision = 14, scale = -3)
		private BigInteger generalInteger;

		@ScaledNumberField(decimalScale = -2)
		@Column(precision = 14, scale = -3)
		private BigInteger bothInteger;
	}

	@Entity(name = IdWithScale.NAME)
	@Indexed(index = IdWithScale.NAME)
	public static class IdWithScale {
		public static final String NAME = "id_with_scale";

		@Id
		@GenericField
		@Column(name = "id", nullable = false, precision = 18)
		private BigInteger id;

		@GenericField
		@Column(name = "other", nullable = false, precision = 18)
		private BigInteger other;

		@GenericField
		private BigDecimal decimalImplicit;
	}

	@Entity(name = DecimalIdWithScale.NAME)
	@Indexed(index = DecimalIdWithScale.NAME)
	public static class DecimalIdWithScale {
		public static final String NAME = "decimal_id_with_scale";

		@Id
		@GenericField
		private BigDecimal id;
	}
}
