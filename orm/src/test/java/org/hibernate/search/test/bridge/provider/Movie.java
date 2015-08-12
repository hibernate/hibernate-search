/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.bridge.provider;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Movie {
	@Id @GeneratedValue
	private Long id;

	private String title;

	public Movie() {
	}

	public Movie(String title) {
		this.title = title;
	}

	@Override
	public String toString() {
		return title;
	}
}
