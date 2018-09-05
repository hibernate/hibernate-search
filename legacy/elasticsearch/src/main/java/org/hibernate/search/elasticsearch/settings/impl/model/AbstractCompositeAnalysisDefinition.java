/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/**
 * A superclass to both {@link AnalyzerDefinition} and {@link NormalizerDefinition}.
 *
 * @author Yoann Rodiere
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
