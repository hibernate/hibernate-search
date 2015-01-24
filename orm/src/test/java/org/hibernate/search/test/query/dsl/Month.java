/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.query.dsl;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.Resolution;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
@ClassBridge(impl = MonthClassBridge.class)
public class Month {

	public Month() {
	}

	public Month(String name, int monthValue, String mythology, String history, Date estimatedCreation) {
		this.name = name;
		this.mythology = mythology;
		this.history = history;
		this.estimatedCreation = estimatedCreation;
		this.monthValue = monthValue;
	}

	public Month(String name, int monthValue, String mythology, String history, Date estimatedCreation, double raindropInMm) {
		this( name, monthValue, mythology, history, estimatedCreation );
		this.raindropInMm = raindropInMm;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	private Integer id;

	@Fields({
			@Field(analyze = Analyze.NO, norms = Norms.NO),
			@Field(analyze = Analyze.NO,
					norms = Norms.NO,
					name = "monthRomanNumber",
					bridge = @FieldBridge(impl = RomanNumberFieldBridge.class))
	})
	public int getMonthValue() {
		return monthValue;
	}

	public void setMonthValue(int monthValue) {
		this.monthValue = monthValue;
	}

	private int monthValue;

	@Field
	public double raindropInMm;

	@Field
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private String name;

	@Fields({
			@Field,
			@Field(name = "mythology_stem", analyzer = @Analyzer(definition = "stemmer")),
			@Field(name = "mythology_ngram", analyzer = @Analyzer(definition = "ngram"))
	})
	public String getMythology() {
		return mythology;
	}

	public void setMythology(String mythology) {
		this.mythology = mythology;
	}

	private String mythology;

	@Field
	public String getHistory() {
		return history;
	}

	public void setHistory(String history) {
		this.history = history;
	}

	private String history;

	@Field(analyze = Analyze.NO)
	@DateBridge(resolution = Resolution.MINUTE)
	public Date getEstimatedCreation() {
		return estimatedCreation;
	}

	public void setEstimatedCreation(Date estimatedCreation) {
		this.estimatedCreation = estimatedCreation;
	}

	private Date estimatedCreation;

}
