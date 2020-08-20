/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.nullindexed;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Davide D'Alto
 */
@Entity
@Indexed
public class Man {

	static final String NO_PET = "NO_PET";

	@Id
	@GeneratedValue
	@DocumentId
	private Integer id;

	@Field
	private String name;

	@OneToOne
	@IndexedEmbedded( indexNullAs = NO_PET )
	private Pet pet;

	@OneToOne
	@IndexedEmbedded
	private Woman partner;

	public Man() {
	}

	public Man(String name) {
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Pet getPet() {
		return pet;
	}

	public void setPet(Pet pet) {
		this.pet = pet;
	}

	public Woman getPartner() {
		return partner;
	}

	public void setPartner(Woman partner) {
		this.partner = partner;
	}

}
