/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.polymorphism;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

@Entity
@Indexed
class Level2 {

	@Id
	@DocumentId
	@GeneratedValue
	private Integer id;

	@OneToOne(mappedBy = "level2Parent")
	@IndexedEmbedded
	private Level3 level3Child;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Level3 getLevel3Child() {
		return level3Child;
	}

	public void setLevel3Child(Level3 level3Child) {
		this.level3Child = level3Child;
	}

}
