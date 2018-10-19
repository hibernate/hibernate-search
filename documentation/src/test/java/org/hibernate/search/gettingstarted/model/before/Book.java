/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.gettingstarted.model.before;

// tag::book-entity-before-hsearch-gettingstarted[]
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

@Entity
public class Book {

	@Id
	@GeneratedValue
	private Integer id;

	private String title;

	private String subtitle;

	@ManyToMany
	private Set<Author> authors = new HashSet<Author>();

	public Book() {
	}

	// ...
}
// end::book-entity-before-hsearch-gettingstarted[]
