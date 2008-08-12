// $Id:$
package org.hibernate.search.test.embedded;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * 
 * @author Hardy Ferentschik
 */
@Entity
@Indexed
public class StateCandidate implements Person {

	@Id @GeneratedValue
	@DocumentId
	private int id;
	
	@Field
	private String name;
	
	@OneToOne(cascade = CascadeType.ALL)
	private Address address;
	
	@IndexedEmbedded
	@OneToOne(cascade = CascadeType.ALL)
	private State state;
	
	public State getState() {
		return state;
	}

	public void setState( State state ) {
		this.state = state;
	}

	public Address getAddress() {
		return address;
	}

	public String getName() {
		return name;
	}

	public void setAddress( Address address ) {
		this.address = address;

	}

	public void setName( String name ) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId( int id ) {
		this.id = id;
	}
}
