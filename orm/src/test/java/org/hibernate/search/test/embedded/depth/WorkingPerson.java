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
package org.hibernate.search.test.embedded.depth;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

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
public class WorkingPerson {

	@Id
	@Column(name = "ID")
	public Integer id;

	@Field(analyze = Analyze.NO)
	public String name;

	@ManyToMany
	@JoinTable(name = "FAMILY",
		joinColumns = @JoinColumn(name = "PARENT_ID", referencedColumnName = "ID"),
		inverseJoinColumns = @JoinColumn(name = "CHILD_ID", referencedColumnName = "ID")
	)
	@IndexedEmbedded(depth = 3)
	public Set<WorkingPerson> parents = new HashSet<WorkingPerson>();

	@ManyToMany(mappedBy = "parents")
	@ContainedIn
	public Set<WorkingPerson> children = new HashSet<WorkingPerson>();

	@ManyToMany
	@JoinTable(name = "WORK",
		joinColumns = @JoinColumn(name = "EMPLOYEE_ID", referencedColumnName = "ID"),
		inverseJoinColumns = @JoinColumn(name = "EMPLOYER_ID", referencedColumnName = "ID")
	)
	@IndexedEmbedded(depth = 1)
	public Set<WorkingPerson> employees = new HashSet<WorkingPerson>();

	@ManyToMany(mappedBy = "employees")
	@ContainedIn
	public Set<WorkingPerson> employers = new HashSet<WorkingPerson>();

	public WorkingPerson() {
	}

	public WorkingPerson(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public void addParents(WorkingPerson... persons) {
		for ( WorkingPerson p : persons ) {
			this.parents.add( p );
			p.children.add( this );
		}
	}

	public void addEmployees(WorkingPerson... persons) {
		for ( WorkingPerson p : persons ) {
			this.employees.add( p );
			p.employers.add( this );
		}
	}
}
