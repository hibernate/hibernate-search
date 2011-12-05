/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	@IndexedEmbedded(depth = 2)
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
