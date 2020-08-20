/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.embedded.nullindexed;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Davide D'Alto
 */
@Entity
public class Pet {

	@Id
	@GeneratedValue
	@DocumentId
	private Integer id;

	@Field
	private String name;

	@OneToMany
	@IndexedEmbedded( prefix = "pups.", indexNullAs = IndexedEmbedded.DEFAULT_NULL_TOKEN )
	private List<Puppy> puppies = new ArrayList<Puppy>();

	@ElementCollection
	@IndexedEmbedded( prefix = "tricks_", indexNullAs = IndexedEmbedded.DEFAULT_NULL_TOKEN )
	private List<Trick> tricks = new ArrayList<Trick>();

	public Pet() {
	}

	public Pet(String name) {
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

	public List<Puppy> getPuppies() {
		return puppies;
	}

	public void setPuppies(List<Puppy> puppies) {
		this.puppies = puppies;
	}

	public Pet addPuppy(Puppy puppy) {
		this.puppies.add( puppy );
		return this;
	}

	public List<Trick> getTricks() {
		return tricks;
	}

	public void setTricks(List<Trick> tricks) {
		this.tricks = tricks;
	}

	public Pet addTrick(Trick trick) {
		this.tricks.add( trick );
		return this;
	}

}
