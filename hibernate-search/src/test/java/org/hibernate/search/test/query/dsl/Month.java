package org.hibernate.search.test.query.dsl;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
public class Month {
	public Month() {}
	
	public Month(String name, String mythology, String history) {
		this.name = name;
		this.mythology = mythology;
		this.history = history;
	}

	@Id @GeneratedValue
	public Integer getId() { return id; }
	public void setId(Integer id) { this.id = id; }
	private Integer id;

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

}

