package org.hibernate.search.test.analyzer;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed(index="idx1")
@Analyzer(impl = Test1Analyzer.class)
public class AlarmEntity {
	
	@Id
	@GeneratedValue
	@DocumentId
	private Integer id;

	@Field(index = Index.TOKENIZED)
	private String property;

	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}

	public String getProperty() {
		return property;
	}
	public void setProperty(String property) {
		this.property = property;
	}

}
