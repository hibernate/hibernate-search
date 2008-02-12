//$Id$
package org.hibernate.search.test.embedded;

import javax.persistence.Embeddable;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.annotations.Parent;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class Owner implements Person {
	@Field(index = Index.TOKENIZED)
	private String name;

	@Parent
	@IndexedEmbedded //play the lunatic user
	private Address address;


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}
}
