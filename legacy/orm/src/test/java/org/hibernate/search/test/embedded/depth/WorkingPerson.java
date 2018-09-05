/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
