/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.update;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Store;


@Entity
public class Dad {
	private Long id;
	private String name;
	private Grandpa grandpa;
	private Set<Son> sons = new HashSet<Son>();

	public Dad() {
	}

	public Dad(String name) {
		this.name = name;
	}

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	private void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne
	public Grandpa getGrandpa() {
		return grandpa;
	}

	public void setGrandpa(Grandpa grandpa) {
		this.grandpa = grandpa;
	}

	@Field(store = Store.YES)
	@Transient
	public Long getGrandpaId() {
		return grandpa != null ? grandpa.getId() : null;
	}

	@ContainedIn
	@OneToMany
	public Set<Son> getSons() {
		return sons;
	}

	private void setSons(Set<Son> sons) {
		this.sons = sons;
	}

	public boolean add(Son son) {
		son.setDad( this );
		return sons.add( son );
	}
}


