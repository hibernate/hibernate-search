/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.nested;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class Person {
	@Id
	@GeneratedValue
	private long id;

	String name;

	@IndexedEmbedded(includeEmbeddedObjectId = true)
	@ManyToMany(cascade = { CascadeType.ALL })
	private List<Place> placesVisited;

	public Person() {
		placesVisited = new ArrayList<Place>( 0 );
	}

	public Person(String name) {
		this();
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public List<Place> getPlacesVisited() {
		return placesVisited;
	}

	public void addPlaceVisited(Place place) {
		placesVisited.add( place );
	}

	public long getId() {
		return id;
	}
}
