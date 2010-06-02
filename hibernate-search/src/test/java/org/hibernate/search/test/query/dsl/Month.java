package org.hibernate.search.test.query.dsl;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Month {

	public Month() {}
	
	public Month(String name, int monthValue, String mythology, String history, Date estimatedCreation) {
		this.name = name;
		this.mythology = mythology;
		this.history = history;
		this.estimatedCreation = estimatedCreation;
		this.monthValue = monthValue;
	}

	@Id @GeneratedValue
	public Integer getId() { return id; }
	public void setId(Integer id) { this.id = id; }
	private Integer id;

	@Field(index = Index.NO_NORMS)
	public int getMonthValue() { return monthValue; }
	public void setMonthValue(int monthValue) { this.monthValue = monthValue; }
	private int monthValue;

	@Field
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	private String name;

	@Fields( {
			@Field,
			@Field(name="mythology_stem", analyzer = @Analyzer(definition = "stemmer") ),
			@Field(name="mythology_ngram", analyzer = @Analyzer(definition = "ngram") )
	})
	public String getMythology() { return mythology; }
	public void setMythology(String mythology) { this.mythology = mythology; }
	private String mythology;

	@Field
	public String getHistory() { return history; }
	public void setHistory(String history) { this.history = history; }
	private String history;

	@Field(index = Index.UN_TOKENIZED) @DateBridge(resolution = Resolution.MINUTE)
	public Date getEstimatedCreation() { return estimatedCreation; }
	public void setEstimatedCreation(Date estimatedCreation) { this.estimatedCreation = estimatedCreation; }
	private Date estimatedCreation;

}

