/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.serialization;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.solr.analysis.NGramFilterFactory;
import org.apache.solr.analysis.StandardTokenizerFactory;

import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
@Entity
@Indexed
@AnalyzerDef(name = "ngram",
		tokenizer = @TokenizerDef( factory = StandardTokenizerFactory.class),
		filters = @TokenFilterDef( factory = NGramFilterFactory.class,
		params = {
		@Parameter(name = "minGramSize", value = "3"),
		@Parameter(name = "maxGramSize", value = "3") })
		)
public class RemoteEntity {
	@Id @DocumentId @GeneratedValue
	public Integer getId() { return id; }
	public void setId(Integer id) { this.id = id; }
	private Integer id;

	@Field(store = Store.YES, termVector = TermVector.WITH_POSITION_OFFSETS, boost = @Boost(23f) )
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	private String name;

	@Field(analyzer = @Analyzer(definition = "ngram")) @NumericField(precisionStep = 2)
	public Float getApproximation() { return approximation; }
	public void setApproximation(Float approximation) { this.approximation = approximation; }
	private Float approximation;
}
