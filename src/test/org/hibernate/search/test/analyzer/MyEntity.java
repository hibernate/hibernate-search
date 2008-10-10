//$Id$
package org.hibernate.search.test.analyzer;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Embedded;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed(index="idx1")
@Analyzer(impl = Test1Analyzer.class)
public class MyEntity {
	@Id
	@GeneratedValue
	@DocumentId
	private Integer id;

	@Field(index = Index.TOKENIZED)
	private String entity;

	@Field(index = Index.TOKENIZED)
	@Analyzer(impl = Test2Analyzer.class)
	private String property;

	@Field(index = Index.TOKENIZED, analyzer = @Analyzer(impl = Test3Analyzer.class) )
	@Analyzer(impl = Test2Analyzer.class)
	private String field;

	@IndexedEmbedded
	@Embedded
	private MyComponent component;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getEntity() {
		return entity;
	}

	public void setEntity(String entity) {
		this.entity = entity;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public MyComponent getComponent() {
		return component;
	}

	public void setComponent(MyComponent component) {
		this.component = component;
	}
}
