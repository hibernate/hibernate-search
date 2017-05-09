/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.filter.deprecated;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FilterCacheModeType;
import org.hibernate.search.annotations.FullTextFilterDef;

/**
 * @author Davide D'Alto
 */
@MappedSuperclass
@FullTextFilterDef(
	name = "roleFilter",
	impl = RoleFilterFactory.class,
	cache = FilterCacheModeType.NONE
)
public class Employee {

	public enum Role {
		CONSULTANT, DEVELOPER, ADMINISTRATOR
	}

	@Id
	private Integer id;

	@Field(analyze = Analyze.YES)
	private String fullName;

	@Field(analyze = Analyze.NO)
	@Enumerated(EnumType.STRING)
	private Role role;

	@Field(analyze = Analyze.NO)
	private String employer;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public String getEmployer() {
		return employer;
	}

	public void setEmployer(String employer) {
		this.employer = employer;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( "Employee [id=" );
		builder.append( id );
		builder.append( ", fullName=" );
		builder.append( fullName );
		builder.append( ", role=" );
		builder.append( role );
		builder.append( ", employer=" );
		builder.append( employer );
		builder.append( "]" );
		return builder.toString();
	}
}
