//$Id$
package org.hibernate.search.test.embedded;

import java.util.Set;
import java.util.HashSet;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.OneToOne;
import javax.persistence.OneToMany;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.ContainedIn;

/**
 * @author Emmanuel Bernard
 */

@Entity
@Indexed
public class Address {
	@Id
	@GeneratedValue
	@DocumentId
	private Long id;

	@Field(index= Index.TOKENIZED)
	private String street;

	@IndexedEmbedded(depth = 1, prefix = "ownedBy_")
	private Owner ownedBy;

	@OneToMany(mappedBy = "address")
	@ContainedIn
	private Set<Tower> towers = new HashSet<Tower>();


	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public Owner getOwnedBy() {
		return ownedBy;
	}

	public void setOwnedBy(Owner ownedBy) {
		this.ownedBy = ownedBy;
	}


	public Set<Tower> getTowers() {
		return towers;
	}

	public void setTowers(Set<Tower> towers) {
		this.towers = towers;
	}
}
