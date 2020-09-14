/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.graph;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;

@Entity
@Indexed
public class Person implements Serializable {

	private Long id;
	private Set<ParentOfBirthEvent> parentOfBirthEvents;
	private Event birthEvent;
	private String name;

	public Person() {
		birthEvent = new Event();
		birthEvent.getChildren().add( this );
		parentOfBirthEvents = new HashSet<ParentOfBirthEvent>();
	}

	@DocumentId
	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ContainedIn
	@OneToMany(cascade = { CascadeType.ALL })
	public Set<ParentOfBirthEvent> getParentOfBirthEvents() {
		return parentOfBirthEvents;
	}

	public void setParentOfBirthEvents(Set<ParentOfBirthEvent> parentOfBirthEvents) {
		this.parentOfBirthEvents = parentOfBirthEvents;
	}

	@IndexedEmbedded(depth = 4)
	@ManyToOne(cascade = { CascadeType.ALL }, optional = false)
	public Event getBirthEvent() {
		return birthEvent;
	}

	public void setBirthEvent(Event birthEvent) {
		this.birthEvent = birthEvent;
	}

	@Field(store = Store.YES)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
