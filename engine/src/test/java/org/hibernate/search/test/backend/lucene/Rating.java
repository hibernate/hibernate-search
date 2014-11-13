/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend.lucene;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;

@Indexed
public class Rating {

	@Field(store = Store.YES)
	private String agency;

	@NumericField(forField = "rate")
	@Field(store = Store.YES)
	private Double rate;

	private static final RandomGenerator randomGenerator = RandomGenerator.withDefaults();

	public Rating(String agency, Double rate) {
		this.agency = agency;
		this.rate = rate;
	}

	public static Rating random() {
		return new Rating( randomGenerator.generateRandomWord(), randomGenerator.randomDouble() );
	}

	@Override
	public String toString() {
		return "Rating{" +
				"agency='" + agency + '\'' +
				", rate=" + rate +
				'}';
	}
}
