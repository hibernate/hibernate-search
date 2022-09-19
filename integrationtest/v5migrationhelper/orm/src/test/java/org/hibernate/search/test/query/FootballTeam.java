/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author Gustavo Fernandes
 */
@Entity
@Indexed
public class FootballTeam {

	public FootballTeam() {
	}

	public FootballTeam(int id, String name, double debtInMillions, int nrTitles) {
		this.id = id;
		this.name = name;
		this.debtInMillions = debtInMillions;
		this.nrTitles = nrTitles;
	}

	@Id
	private int id;

	@Field(store = Store.YES)
	private String name;

	@Field(store = Store.YES)
	private double debtInMillions;

	@Field(store = Store.YES)
	private int nrTitles;

}
