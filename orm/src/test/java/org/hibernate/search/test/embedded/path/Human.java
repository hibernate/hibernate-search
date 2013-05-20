/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.test.embedded.path;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Davide D'Alto
 */
@Entity
@Indexed
public class Human {

	public int id;

	public String name;

	public Set<Human> parents = new HashSet<Human>();

	public Human child;

	private String surname;

	public Human() {
	}

	public Human(String name, String surname) {
		this.name = name;
		this.surname = surname;
	}

	public void addParents(Human father, Human mother) {
		this.parents.add( father );
		this.parents.add( mother );
		father.child = this;
		mother.child = this;
	}

	@Transient
	public String getFullname() {
		return name + " " + surname;
	}

	@Id
	@GeneratedValue
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Field(analyze = Analyze.NO)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Field(analyze = Analyze.NO)
	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	@OneToMany
	@IndexedEmbedded(depth = 2, includePaths = { "parents.parents.name" })
	public Set<Human> getParents() {
		return parents;
	}

	public void setParents(Set<Human> parents) {
		this.parents = parents;
	}

	@ContainedIn
	@ManyToOne
	public Human getChild() {
		return child;
	}

	public void setChild(Human child) {
		this.child = child;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( child == null ) ? 0 : child.hashCode() );
		result = prime * result + id;
		result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
		result = prime * result + ( ( surname == null ) ? 0 : surname.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		Human other = (Human) obj;
		if ( child == null ) {
			if ( other.child != null ) {
				return false;
			}
		}
		else {
			if ( !child.equals( other.child ) ) {
				return false;
			}
		}
		if ( id != other.id ) {
			return false;
		}
		if ( name == null ) {
			if ( other.name != null ) {
				return false;
			}
		}
		else {
			if ( !name.equals( other.name ) ) {
				return false;
			}
		}
		if ( surname == null ) {
			if ( other.surname != null ) {
				return false;
			}
		}
		else {
			if ( !surname.equals( other.surname ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return name + " " + surname;
	}

}
