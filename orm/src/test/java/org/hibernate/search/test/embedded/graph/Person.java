/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
