/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend.serialization;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * When Hibernate Search is used in a clustering context, it requires a SerializationProvider.
 * This test checks that the error message is giving some details to the user if a SerializationProvider is not registered.
 *
 * @author Davide D'Alto
 */
@TestForIssue(jiraKey = "HSEARCH-1815")
public class SerializationProviderMissingTest {

	@Rule
	public SearchFactoryHolder factoryHolder = new SearchFactoryHolder( Book.class );

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testExceptionMessage() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000275" );
		thrown.expectMessage( "hibernate-search-serialization-avro" );

		ExtendedSearchIntegrator searchFactory = factoryHolder.getSearchFactory();
		searchFactory.getServiceManager().requestService( LuceneWorkSerializer.class );
	}

	@Indexed(index = "books")
	private static class Book {
		@DocumentId long id;
		@Field String title;
	}
}
