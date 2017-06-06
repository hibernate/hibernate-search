/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.field;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.test.util.impl.ExpectedLog4jLog;
import org.hibernate.search.testsupport.TestConstants;
import org.junit.Rule;
import org.junit.Test;


/**
 * Tests related to the {@code Field} annotation and its options
 *
 * @author Hardy Ferentschik
 */
public class TokenizationTest {
	private static final String DEFAULT_FIELD_NAME = "default";

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Test
	public void testWarningLoggedForInconsistentFieldConfiguration() throws Exception {
		Configuration config = new Configuration();
		config.addAnnotatedClass( Product.class );

		config.setProperty( "hibernate.search.lucene_version", TestConstants.getTargetLuceneVersion().toString() );
		config.setProperty( "hibernate.search.default.directory_provider", "local-heap" );

		logged.expectMessage( "HSEARCH000120", Product.class.getName(), DEFAULT_FIELD_NAME );

		config.buildSessionFactory();
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
