/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.gettingstarted.model.after;

// tag::book-entity-after-hsearch-gettingstarted[]
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.hibernate.search.engine.backend.document.model.dsl.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

@Entity
@Indexed
public class Book {

	@Id
	@GeneratedValue
	private Integer id;

	@GenericField(projectable = Projectable.NO)
	private String title;

	@GenericField(projectable = Projectable.NO)
	private String subtitle;

	@IndexedEmbedded
	@ManyToMany
	private Set<Author> authors = new HashSet<Author>();

	public Book() {
	}

	// ...
}
// end::book-entity-after-hsearch-gettingstarted[]
