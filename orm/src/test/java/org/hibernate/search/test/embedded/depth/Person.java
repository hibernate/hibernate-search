/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.depth;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Sanne Grinovero
 */
@Entity
@Indexed
public class Person {

	public Person() {
	}

	public Person(long id, String name) {
		this.id = id;
		this.name = name;
	}

	public Long id;

	public String name;

	public Set<Person> parents = new HashSet<Person>();

	public Set<Person> children = new HashSet<Person>();

	public void addParents(Person... persons) {
		for ( Person p : persons ) {
			parents.add( p );
			p.children.add( this );
		}
	}

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Field(analyze = Analyze.NO)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@IndexedEmbedded(depth = 2)
	@ManyToMany
	public Set<Person> getParents() {
		return parents;
	}

	public void setParents(Set<Person> parents) {
		this.parents = parents;
	}

	@ContainedIn
	@ManyToMany(mappedBy = "parents")
	public Set<Person> getChildren() {
		return children;
	}

	public void setChildren(Set<Person> children) {
		this.children = children;
	}

}
