/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.backend.lucene;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Indexed
public class Quote {

	private static final RandomGenerator randomGenerator = RandomGenerator.withDefaults();
	private static final AtomicInteger counter = new AtomicInteger( 0 );
	private static final int MAX_RATINGS = 5;

	private List<Rating> ratings = new ArrayList<Rating>();

	@Field(store = Store.YES)
	private String description;

	@DocumentId
	private Integer id;

	public Quote(Integer id, String description) {
		this.description = description;
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	@IndexedEmbedded
	private List<Rating> getRatings() {
		return ratings;
	}

	public void addRating(Rating rating) {
		ratings.add( rating );
	}

	public static Quote random() {
		return getRandomQuote( null );
	}

	public static Quote random(int id) {
		return getRandomQuote( id );
	}

	private static Quote getRandomQuote(Integer id) {
		int docId = id == null ? counter.incrementAndGet() : id;
		Quote quote = new Quote( docId, randomGenerator.generateRandomPhrase() );
		addRatings( quote );
		return quote;
	}

	private static void addRatings(Quote quote) {
		for ( int i = 0; i < randomGenerator.randomIntNotZero( MAX_RATINGS ); i++ ) {
			quote.addRating( Rating.random() );
		}
	}

	@Override
	public String toString() {
		return "Quote{" +
				"name='" + description + '\'' +
				", id=" + id +
				", ratings=" + ratings +
				'}';
	}

}
