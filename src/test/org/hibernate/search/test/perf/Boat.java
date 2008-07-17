// $Id$
package org.hibernate.search.test.perf;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Store;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Boat {
	@Id
	@GeneratedValue
	@DocumentId
	public Integer id;
	@Field(store= Store.YES)
	public String name;
	@Field
	public String description;

	public Boat() {}

	public Boat(String name, String description) {
		this.name = name;
		this.description = description;
	}
}
