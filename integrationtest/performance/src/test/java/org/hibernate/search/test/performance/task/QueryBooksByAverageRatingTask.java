/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.performance.task;

import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.test.performance.model.Book;
import org.hibernate.search.test.performance.scenario.TestContext;

/**
 * @author Tomas Hradec
 */
public class QueryBooksByAverageRatingTask extends AbstractTask {

	public QueryBooksByAverageRatingTask(TestContext ctx) {
		super( ctx );
	}

	@Override
	@SuppressWarnings({ "unchecked", "unused" })
	protected void execute(FullTextSession fts) {
		Query q = fts.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Book.class )
				.get()
				.range()
				.onField( "rating" )
				.from( 40.0f )
				.to( 60.0f )
				.createQuery();

		List<Book> result = fts.createFullTextQuery( q, Book.class )
				.setSort( new Sort( new SortField( "rating", SortField.Type.FLOAT, true ) ) )
				.list();
	}

}
