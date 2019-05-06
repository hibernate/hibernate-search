/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;

import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeContext;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScaledNumberField;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;

public class ScaledNumberFieldIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper( MethodHandles.lookup() );

	@Test
	public void validDecimalScales() {

		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			BigDecimal scaled;
			BigDecimal unscaled;
			BigDecimal defaultScaled;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@ScaledNumberField(decimalScale = 7)
			public BigDecimal getScaled() {
				return scaled;
			}

			@ScaledNumberField(decimalScale = 0)
			public BigDecimal getUnscaled() {
				return unscaled;
			}

			@ScaledNumberField
			public BigDecimal getDefaultScaled() {
				return defaultScaled;
			}
		}

		// Schema
		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "scaled", BigDecimal.class, f -> f.decimalScale( 7 ) )
				.field( "unscaled", BigDecimal.class, f -> f.decimalScale( 0 ) )
				.field( "defaultScaled", BigDecimal.class )
		);
		setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void invalidFieldType() {

		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			Integer notScalable;

			@DocumentId
			public Integer getId() {
				return id;
			}

			@ScaledNumberField
			public Integer getNotScalable() {
				return notScalable;
			}
		}

		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".notScalable" )
						.failure(
								"This property is mapped to a scaled number field, but with a value bridge that creates a non-BigDecimal or otherwise incompatible field",
								"bind() method returned context '",
								"expected '" + ScaledNumberIndexFieldTypeContext.class.getName() + "'"
						)
						.build()
				);
	}

}
