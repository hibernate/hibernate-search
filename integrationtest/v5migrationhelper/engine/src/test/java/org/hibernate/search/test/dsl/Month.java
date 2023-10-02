/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.dsl;

import java.util.Date;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Normalizer;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.testsupport.AnalysisNames;

/**
 * @author Emmanuel Bernard
 */
@Indexed
class Month {

	public Month(String name, int monthValue, String mythology, String history, Date estimatedCreation) {
		this.id = monthValue;
		this.name = name;
		this.mythology = mythology;
		this.history = history;
		this.estimatedCreation = estimatedCreation;
		this.monthValue = monthValue;
	}

	public Month(String name, int monthValue, String mythology, String history, Date estimatedCreation, double raindropInMm,
			String htmlDescription) {
		this( name, monthValue, mythology, history, estimatedCreation );
		this.raindropInMm = raindropInMm;
		this.htmlDescription = htmlDescription;
	}

	@DocumentId
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	private Integer id;

	@Fields({
			@Field(analyze = Analyze.NO, norms = Norms.NO)
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

	@Field(normalizer = @Normalizer(definition = "lower"))
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private String name;

	@Fields({
			@Field,
			@Field(name = "mythology_stem",
					analyzer = @Analyzer(definition = AnalysisNames.ANALYZER_STANDARD_STANDARD_LOWERCASE_STOP_STEMMER_ENGLISH)),
			@Field(name = "mythology_ngram",
					analyzer = @Analyzer(definition = AnalysisNames.ANALYZER_STANDARD_STANDARD_LOWERCASE_STOP_NGRAM_3)),
			// This field must exist in order for tests to pass with the Elasticsearch integration... See HSEARCH-2534
			@Field(name = "mythology_same_base_as_ngram",
					analyzer = @Analyzer(definition = AnalysisNames.ANALYZER_STANDARD_STANDARD_LOWERCASE_STOP)),
			@Field(name = "mythology_normalized",
					normalizer = @Normalizer(definition = AnalysisNames.NORMALIZER_LOWERCASE))
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

	@Fields({
			@Field,
			@Field(name = "htmlDescription_htmlStrip",
					analyzer = @Analyzer(definition = AnalysisNames.ANALYZER_STANDARD_HTML_STRIP_ESCAPED_LOWERCASE))
	})
	public String getHtmlDescription() {
		return htmlDescription;
	}

	public void setHtmlDescription(String htmlDescription) {
		this.htmlDescription = htmlDescription;
	}

	private String htmlDescription;
}
