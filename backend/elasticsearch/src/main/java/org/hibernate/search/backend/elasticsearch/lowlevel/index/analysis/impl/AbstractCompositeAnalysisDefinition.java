/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/**
 * A superclass to both {@link AnalyzerDefinition} and {@link NormalizerDefinition}.
 *
 */
public abstract class AbstractCompositeAnalysisDefinition extends AnalysisDefinition {

	@SerializedName("filter")
	private List<String> tokenFilters;

	@SerializedName("char_filter")
	private List<String> charFilters;

	public List<String> getTokenFilters() {
		return tokenFilters;
	}

	public void setTokenFilters(List<String> tokenFilters) {
		this.tokenFilters = tokenFilters;
	}

	public void addTokenFilter(String tokenFilter) {
		getInitializedTokenFilters().add( tokenFilter );
	}

	private List<String> getInitializedTokenFilters() {
		if ( tokenFilters == null ) {
			tokenFilters = new ArrayList<>();
		}
		return tokenFilters;
	}

	public List<String> getCharFilters() {
		return charFilters;
	}

	public void setCharFilters(List<String> charFilters) {
		this.charFilters = charFilters;
	}

	public void addCharFilter(String charFilter) {
		getInitializedCharFilters().add( charFilter );
	}

	private List<String> getInitializedCharFilters() {
		if ( charFilters == null ) {
			charFilters = new ArrayList<>();
		}
		return charFilters;
	}

	@Override
	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson( this );
	}

}
