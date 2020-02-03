/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.CharFilterDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.NormalizerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenFilterDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenizerDefinition;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

public class Analysis {

	@SerializedName("analyzer")
	private Map<String, AnalyzerDefinition> analyzers;

	@SerializedName("normalizer")
	private Map<String, NormalizerDefinition> normalizers;

	@SerializedName("tokenizer")
	private Map<String, TokenizerDefinition> tokenizers;

	@SerializedName("filter")
	private Map<String, TokenFilterDefinition> tokenFilters;

	@SerializedName("char_filter")
	private Map<String, CharFilterDefinition> charFilters;

	public boolean isEmpty() {
		return ! hasContent( analyzers, normalizers, tokenizers, tokenFilters, charFilters );
	}

	private boolean hasContent(Map<?, ?> ... maps) {
		for ( Map<?, ?> map : maps ) {
			if ( map != null && !map.isEmpty() ) {
				return true;
			}
		}
		return false;
	}

	public Map<String, AnalyzerDefinition> getAnalyzers() {
		return analyzers;
	}

	public void setAnalyzers(Map<String, AnalyzerDefinition> analyzers) {
		this.analyzers = analyzers;
	}

	public Map<String, NormalizerDefinition> getNormalizers() {
		return normalizers;
	}

	public void setNormalizers(Map<String, NormalizerDefinition> normalizers) {
		this.normalizers = normalizers;
	}

	public Map<String, TokenizerDefinition> getTokenizers() {
		return tokenizers;
	}

	public void setTokenizers(Map<String, TokenizerDefinition> tokenizers) {
		this.tokenizers = tokenizers;
	}

	public Map<String, TokenFilterDefinition> getTokenFilters() {
		return tokenFilters;
	}

	public void setTokenFilters(Map<String, TokenFilterDefinition> tokenFilters) {
		this.tokenFilters = tokenFilters;
	}

	public Map<String, CharFilterDefinition> getCharFilters() {
		return charFilters;
	}

	public void setCharFilters(Map<String, CharFilterDefinition> charFilters) {
		this.charFilters = charFilters;
	}

	@Override
	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson( this );
	}
}
