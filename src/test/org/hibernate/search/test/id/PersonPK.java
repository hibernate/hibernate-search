// $Id$
package org.hibernate.search.test.id;

import java.io.Serializable;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class PersonPK implements Serializable {
	private String firstName;
	private String lastName;

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof PersonPK ) ) return false;

		PersonPK personPK = (PersonPK) o;

		if ( firstName != null ? !firstName.equals( personPK.firstName ) : personPK.firstName != null ) return false;
		if ( lastName != null ? !lastName.equals( personPK.lastName ) : personPK.lastName != null ) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = ( firstName != null ? firstName.hashCode() : 0 );
		result = 31 * result + ( lastName != null ? lastName.hashCode() : 0 );
		return result;
	}
}
