/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.sorting;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Davide D'Alto
 */
@Entity
@Indexed
class Hero {

	@Id
	private Integer id;

	@OneToOne(mappedBy = "hero")
	@IndexedEmbedded(includePaths = "name")
	private Villain villain;

	public Hero() {
	}

	public Hero(Integer id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Villain getVillain() {
		return villain;
	}

	public void setVillain(Villain villain) {
		this.villain = villain;
	}

}
