/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.configuration.field;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.test.util.BytemanHelper;

import static junit.framework.Assert.assertEquals;


/**
 * Tests related to the {@code Field} annotation and its options
 *
 * @author Hardy Ferentschik
 */
@RunWith(BMUnitRunner.class)
public class TokenizationTest {
	private static final String DEFAULT_FIELD_NAME = "default";

	@Test
	@BMRule(targetClass = "org.hibernate.search.util.logging.impl.Log_$logger",
			targetMethod = "inconsistentFieldConfiguration",
			helper = "org.hibernate.search.test.util.BytemanHelper",
			action = "countInvocation()",
			name = "testWarningLoggedForInconsistentFieldConfiguration")
	public void testWarningLoggedForInconsistentFieldConfiguration() throws Exception {
		Configuration config = new Configuration();
		config.addAnnotatedClass( Product.class );

		config.setProperty( "hibernate.search.lucene_version", TestConstants.getTargetLuceneVersion().name() );
		config.setProperty( "hibernate.search.default.directory_provider", "ram" );
		config.setProperty( "hibernate.search.default.indexBase", TestConstants.getIndexDirectory( TokenizationTest.class ) );

		config.buildSessionFactory();

		assertEquals( "Wrong invocation count", 1, BytemanHelper.getAndResetInvocationCount() );
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
