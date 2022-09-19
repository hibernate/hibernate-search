/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.sorting;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

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

	private String secretIdentity;

	@OneToOne(mappedBy = "hero")
	@IndexedEmbedded(includePaths = "name")
	private Villain villain;

	@OneToOne(mappedBy = "hero")
	@IndexedEmbedded(includePaths = { "id_sort", "name" })
	private Villain sortableVillain;

	public Hero() {
	}

	public Hero(Integer id, String secretIdentity) {
		this.id = id;
		this.secretIdentity = secretIdentity;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getSecretIdentity() {
		return secretIdentity;
	}

	public void setSecretIdentity(String secretIdentity) {
		this.secretIdentity = secretIdentity;
	}

	public Villain getVillain() {
		return villain;
	}

	public void setVillain(Villain villain) {
		this.villain = villain;
	}

	public Villain getSortableVillain() {
		return sortableVillain;
	}

	public void setSortableVillain(Villain sortableVillain) {
		this.sortableVillain = sortableVillain;
	}
}
