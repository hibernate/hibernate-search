// $Id$
package org.hibernate.search.test.session;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Domain {
	@Id
	@DocumentId
	private Integer id;
	@Field
	private String name;
	
	public Domain(){ }
	
	public Domain(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

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
}
