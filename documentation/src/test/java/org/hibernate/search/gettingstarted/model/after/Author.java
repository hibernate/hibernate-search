/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.gettingstarted.model.after;

// tag::author-entity-after-hsearch-gettingstarted[]
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class Author {

	@Id
	@GeneratedValue
	private Integer id;

	@GenericField(sortable = Sortable.YES)
	private String name;

	public Author() {
	}

	// ...
}
// end::author-entity-after-hsearch-gettingstarted[]
