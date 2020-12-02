/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.gson.impl.SerializeExtraProperties;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.CharFilterDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.NormalizerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenFilterDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenizerDefinition;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

@JsonAdapter(AnalysisJsonAdapterFactory.class)
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

	@SerializeExtraProperties
	private Map<String, JsonElement> extraAttributes;

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

	public Map<String, JsonElement> getExtraAttributes() {
		return extraAttributes;
	}

	@Override
	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson( this );
	}

	/**
	 * Merge this analysis with another one.
	 * Any conflict of definition will be solved in favour of the other analysis.
	 * {@link #extraAttributes} will be always overridden.
	 *
	 * @param overridingAnalysis The other analysis definition
	 */
	public void merge(Analysis overridingAnalysis) {
		if ( overridingAnalysis == null ) {
			// nothing to do
			return;
		}

		if ( overridingAnalysis.analyzers != null ) {
			analyzers = new HashMap<>( analyzers );
			analyzers.putAll( overridingAnalysis.analyzers );
		}

		if ( overridingAnalysis.normalizers != null ) {
			normalizers = new HashMap<>( normalizers );
			normalizers.putAll( overridingAnalysis.normalizers );
		}

		if ( overridingAnalysis.tokenizers != null ) {
			tokenizers = new HashMap<>( tokenizers );
			tokenizers.putAll( overridingAnalysis.tokenizers );
		}

		if ( overridingAnalysis.tokenFilters != null ) {
			tokenFilters = new HashMap<>( tokenFilters );
			tokenFilters.putAll( overridingAnalysis.tokenFilters );
		}

		if ( overridingAnalysis.charFilters != null ) {
			charFilters = new HashMap<>( charFilters );
			charFilters.putAll( overridingAnalysis.charFilters );
		}

		extraAttributes = overridingAnalysis.extraAttributes;
	}
}
