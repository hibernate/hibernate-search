// $Id$
package org.hibernate.search.test.embedded;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class State {
	@Id
	@DocumentId
	@GeneratedValue
	private Integer id;
	
	@Field
	private String name;
	
	@ContainedIn
	@OneToOne(mappedBy = "state")
	private StateCandidate candidate;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public StateCandidate getCandidate() {
		return candidate;
	}

	public void setCandidate( StateCandidate candidate ) {
		this.candidate = candidate;
	}
}
