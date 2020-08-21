/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.nested;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Place {
	@Id
	@GeneratedValue
	private Long id;

	@Field
	private String name;

	@OneToOne(cascade = CascadeType.ALL)
	@IndexedEmbedded
	private Address address;

	@ContainedIn
	@ManyToMany(cascade = { CascadeType.ALL }, mappedBy = "placesVisited")
	private Set<Person> visitedBy;

	public Place() {
		this.visitedBy = new HashSet<Person>();
	}

	public Place(String name) {
		this();
		this.name = name;
	}

	public Address getAddress() {
		return address;
	}

	public String getName() {
		return name;
	}

	public Long getId() {
		return id;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public void visitedBy(Person person) {
		visitedBy.add( person );
	}

	public Set<Person> getVisitedBy() {
		return visitedBy;
	}
}
