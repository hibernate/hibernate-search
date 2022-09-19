/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.field;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


/**
 * Tests related to the {@code Field} annotation and its options
 *
 * @author Hardy Ferentschik
 */
public class TokenizationTest extends SearchInitializationTestBase {
	private static final String DEFAULT_FIELD_NAME = "default";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testWarningLoggedForInconsistentFieldConfiguration() throws Exception {
		thrown.expectMessage( "Duplicate index field definition: '" + DEFAULT_FIELD_NAME + "'" );
		thrown.expectMessage( Product.class.getName() );

		init( Product.class );
	}

	@Entity
	@Indexed
	public static class Product {
		@Id
		@GeneratedValue
		private long id;

		@Field(name = DEFAULT_FIELD_NAME, index = Index.YES, analyze = Analyze.NO, store = Store.YES)
		private String productId;

		@Field(name = DEFAULT_FIELD_NAME, index = Index.YES, analyze = Analyze.YES, store = Store.YES)
		private String description;

		public String getProductId() {
			return productId;
		}

		public void setProductId(String productId) {
			this.productId = productId;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}
}
