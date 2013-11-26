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

import java.util.Random;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.test.performance.model.Book;
import org.hibernate.search.test.performance.scenario.TestContext;

/**
 * @author Tomas Hradec
 */
public class UpdateBookRatingTask extends AbstractTask {

	private static final float MAX_RATING = 100;
	private static final Random RANDOM_RATING = new Random();

	public UpdateBookRatingTask(TestContext ctx) {
		super( ctx );
	}

	@Override
	protected void execute(FullTextSession fts) {
		long bookId = ctx.getRandomBookId();
		Book book = (Book) fts.get( Book.class, bookId );
		if ( book != null ) {
			book.setRating( Math.abs( RANDOM_RATING.nextFloat() ) * MAX_RATING );
		}
	}

}
