/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	private List<Rating> ratings = new ArrayList<>();

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
