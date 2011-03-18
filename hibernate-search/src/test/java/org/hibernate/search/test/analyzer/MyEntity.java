/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
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
@Analyzer(impl = AnalyzerForTests1.class)
public class MyEntity {
	@Id
	@GeneratedValue
	@DocumentId
	private Integer id;

	@Field(index = Index.TOKENIZED)
	private String entity;

	@Field(index = Index.TOKENIZED)
	@Analyzer(impl = AnalyzerForTests2.class)
	private String property;

	@Field(index = Index.TOKENIZED, analyzer = @Analyzer(impl = AnalyzerForTests3.class) )
	@Analyzer(impl = AnalyzerForTests2.class)
	private String field;

	@Field(index = Index.UN_TOKENIZED)
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
