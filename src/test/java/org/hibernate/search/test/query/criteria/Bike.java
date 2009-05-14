//$Id$
package org.hibernate.search.test.query.criteria;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.search.annotations.Field;

@Entity
public class Bike {

	@Id
	@GeneratedValue
	private Integer id;

	@Field
	private String kurztext;

	private boolean hasColor = false;

	protected Bike() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getKurztext() {
		return kurztext;
	}

	public void setKurztext(final String kurztext) {
		this.kurztext = kurztext;
	}

	public boolean isHasColor() {
		return hasColor;
	}
}