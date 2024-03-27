/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.configuration.field;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.test.SearchInitializationTestBase;

import org.junit.jupiter.api.Test;

/**
 * Tests related to the {@code Field} annotation and its options
 *
 * @author Hardy Ferentschik
 */
class TokenizationTest extends SearchInitializationTestBase {
	private static final String DEFAULT_FIELD_NAME = "default";

	@Test
	void testWarningLoggedForInconsistentFieldConfiguration() {
		assertThatThrownBy( () -> init( Product.class ) )
				.hasMessageContainingAll(
						"Duplicate index field definition: '" + DEFAULT_FIELD_NAME + "'",
						Product.class.getName()
				);
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
