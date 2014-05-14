/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.criteria;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Julie Ingignoli
 */
@Entity
@Indexed
public class Tractor {

	@Id
	@GeneratedValue
	private Integer id;

	@Field
	private String kurztext;

	private boolean hasColor = true;

	@Field
	private String owner;

	protected Tractor() {
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

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public void removeColor() {
		hasColor = false;
	}
}
