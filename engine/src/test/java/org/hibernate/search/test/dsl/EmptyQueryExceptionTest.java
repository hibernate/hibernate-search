/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.dsl;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.errors.EmptyQueryException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.util.SearchFactoryHolder;
import org.hibernate.search.test.util.ManualTransactionContext;
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
		final SearchFactoryImplementor searchFactory = sfHolder.getSearchFactory();

		Book book = new Book();
		book.title = "Empty Book";
		book.text = "The question is, does an empty book have 'space' tokens in it?";

		Work work = new Work( book, book.title, WorkType.ADD, false );
		ManualTransactionContext tc = new ManualTransactionContext();
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
