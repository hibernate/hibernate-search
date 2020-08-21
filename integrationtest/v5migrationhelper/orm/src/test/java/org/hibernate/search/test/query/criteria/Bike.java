/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.criteria;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

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
