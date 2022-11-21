/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScaledNumberField;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DefaultDecimalScaleMappingIT {

	private static final String INDEX_NAME = "IndexName";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Test
	public void mapping() {
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
}
