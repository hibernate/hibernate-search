/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.mapping;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.math.BigInteger;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.integrationtest.mapper.orm.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Rule;
import org.junit.Test;

public class DecimalScaleMappingIT {

	@Rule
	public OrmSetupHelper setupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	@Test
	public void testFailingWithHint() {
		assertThatThrownBy( () ->
				setupHelper.start().setup( FailingEntity.class )
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Hibernate Search encountered failures during bootstrap",
						"Invalid index field type: missing decimal scale. Define the decimal scale explicitly.",
						"If you used a @*Field annotation here, make sure to use @ScaledNumberField and configure the `decimalScale` attribute as necessary."
				);
	}

	@Entity(name = "failing")
	@Indexed
	public static class FailingEntity {
		@Id
		@Column(name = "id", nullable = false, precision = 18)
		@GenericField
		private BigInteger id;

		public BigInteger id() {
			return id;
		}

		public FailingEntity withId(BigInteger id) {
			this.id = id;
			return this;
		}
	}

}
