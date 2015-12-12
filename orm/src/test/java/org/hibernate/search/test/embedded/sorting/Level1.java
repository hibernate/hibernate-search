/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.sorting;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

@Entity
@Indexed
class Level1 {

	@Id
	@GeneratedValue
	private Integer id;

	@OneToOne(mappedBy = "level1Parent")
	@IndexedEmbedded(includePaths = "name")
	private Level2SortableId level2Child;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Level2SortableId getLevel2Child() {
		return level2Child;
	}

	public void setLevel2Child(Level2SortableId level2Child) {
		this.level2Child = level2Child;
	}

}
