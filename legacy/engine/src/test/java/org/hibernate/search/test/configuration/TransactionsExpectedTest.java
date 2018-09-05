/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.lang.annotation.ElementType;

import org.junit.Assert;
import org.junit.Rule;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.impl.MutableSearchFactory;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Test;

/**
 * Verifies org.hibernate.search.test.util.ManualConfiguration.isTransactionManagerExpected()
 * is applied correctly to the build SearchFactory [HSEARCH-1055].
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
public class TransactionsExpectedTest {

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	@Test
	public void testDefaultImplementation() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		verifyTransactionsExpectedOption( true, cfg );
	}

	@Test
	public void testTransactionsNotExpected() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.setTransactionsExpected( false );
		verifyTransactionsExpectedOption( false, cfg );
	}

	@Test
	public void testTransactionsExpected() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.setTransactionsExpected( true );
		verifyTransactionsExpectedOption( true, cfg );
	}

	private void verifyTransactionsExpectedOption(boolean expectation, SearchConfigurationForTest cfg) {
		SearchMapping mapping = new SearchMapping();
		mapping
			.entity( Document.class ).indexed()
			.property( "id", ElementType.FIELD ).documentId()
			.property( "title", ElementType.FIELD ).field()
			;
		cfg.setProgrammaticMapping( mapping );
		cfg.addClass( Document.class );
		MutableSearchFactory sf = (MutableSearchFactory) integratorResource.create( cfg );
		Assert.assertEquals( expectation, sf.isTransactionManagerExpected() );
		// trigger a SearchFactory rebuild:
		sf.addClasses( Dvd.class );
		// and verify the option is not lost:
		Assert.assertEquals( expectation, sf.isTransactionManagerExpected() );
	}

	public static final class Document {
		long id;
		String title;
	}

	@Indexed
	public static final class Dvd {
		@DocumentId long id;
		@Field String title;
	}

}
