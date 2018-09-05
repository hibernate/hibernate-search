/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.id;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class RegistrationId implements Serializable {

	@Column(name = "STUDENT_ID")
	private int studentId;

	@Column(name = "DEPARTMENT")
	private String department;

	public int getStudentId() {
		return studentId;
	}

	public void setStudentId(int studentId) {
		this.studentId = studentId;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( department == null ) ? 0 : department.hashCode() );
		result = prime * result + studentId;
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
		RegistrationId other = (RegistrationId) obj;
		if ( department == null ) {
			if ( other.department != null ) {
				return false;
			}
		}
		else if ( !department.equals( other.department ) ) {
			return false;
		}
		return studentId == other.studentId;
	}

}
