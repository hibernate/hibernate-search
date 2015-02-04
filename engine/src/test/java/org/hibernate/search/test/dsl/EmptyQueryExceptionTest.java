/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.dsl;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.EmptyQueryException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


/**
 * Verifies an EmptyQueryException is thrown when appropriate.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class EmptyQueryExceptionTest {

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( Book.class );

	@Rule
	public ExpectedException exceptions = ExpectedException.none();

	@Test
	public void verifyExceptionOnNonMeaningfullQueries() {
		final ExtendedSearchIntegrator searchFactory = sfHolder.getSearchFactory();

		Book book = new Book();
		book.title = "Empty Book";
		book.text = "The question is, does an empty book have 'space' tokens in it?";

		Work work = new Work( book, book.title, WorkType.ADD, false );
		TransactionContextForTest tc = new TransactionContextForTest();
		searchFactory.getWorker().performWork( work, tc );
		tc.end();

		QueryBuilder queryBuilder = searchFactory.buildQueryBuilder().forEntity( Book.class ).get();

		exceptions.expect( EmptyQueryException.class );

		queryBuilder.keyword().onField( "text" ).matching( " " ).createQuery();
		// Hence the answer is: a program won't be able to tell you.
	}

	@Indexed
	static class Book {
		@DocumentId
		String title;

		@Field
		String text;
	}

}
