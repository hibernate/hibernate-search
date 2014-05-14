/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed(index = "idx1")
@Analyzer(impl = AnalyzerForTests1.class)
public class MyEntity {
	@Id
	@GeneratedValue
	@DocumentId
	private Integer id;

	@Field
	private String entity;

	@Field
	@Analyzer(impl = AnalyzerForTests2.class)
	private String property;

	@Field(analyzer = @Analyzer(impl = AnalyzerForTests3.class))
	@Analyzer(impl = AnalyzerForTests2.class)
	private String field;

	@Field(analyze = Analyze.NO)
	private String notAnalyzed;

	public String getNotAnalyzed() {
		return notAnalyzed;
	}

	public void setNotAnalyzed(String notAnalyzed) {
		this.notAnalyzed = notAnalyzed;
	}

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
