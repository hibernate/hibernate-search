package org.hibernate.search.test.query;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author: Gustavo Fernandes
 */
@Entity
@Indexed
public class FootballTeam {

	public FootballTeam() {}

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

	@Field( store = Store.YES ) @NumericField
	private double debtInMillions;

	@Field( store = Store.YES)
	private int nrTitles;

}
