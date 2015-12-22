/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.sorting;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.SortableField;

/**
 * @author Davide D'Alto
 */
@Entity
@Indexed
class Villain {

	private static final String ID_SORT = "idSort";

	@Id
	@SortableField
	@GeneratedValue
	private Integer id;

	@OneToOne
	@ContainedIn
	private Hero hero;

	@Field
	private String name;

	public Villain() {
	}

	public Villain(Integer id, String name) {
		super();
		this.id = id;
		this.name = name;
	}

	@Field(name = ID_SORT)
	@SortableField(forField = ID_SORT)
	@NumericField(forField = ID_SORT)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Hero getHero() {
		return hero;
	}

	public void setHero(Hero hero) {
		this.hero = hero;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
