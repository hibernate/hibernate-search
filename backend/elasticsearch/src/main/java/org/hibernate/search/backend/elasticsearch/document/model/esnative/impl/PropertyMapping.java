/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.esnative.impl;

import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

/**
 * An object representing Elasticsearch property mappings, i.e. the mappings of properties inside a type mapping.
 *
 * See https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html#mapping-type
 */
/*
 * CAUTION: JSON serialization is controlled by a specific adapter, which must be
 * updated whenever fields of this class are added, renamed or removed.
 */
@JsonAdapter(PropertyMappingJsonAdapterFactory.class)
public class PropertyMapping extends AbstractTypeMapping {

	private String type;

	/*
	 * Attributes common to multiple datatypes
	 */

	private Boolean index;

	private Boolean norms;

	@SerializedName("doc_values")
	private Boolean docValues;

	private Boolean store;

	/**
	 * The null-value replacement, which can be a string (real string or formatted date), a boolean
	 * or a numeric value, depending on the data type.
	 *
	 * <p>Using JsonPrimitive here instead of Object allows us to take advantage of lazily parsed numbers,
	 * so that long values are not arbitrarily parsed as doubles, which would involve losing some
	 * information. See the difference in behavior between those adapters when parsing numbers:
	 * <ul>
	 * <li>com.google.gson.internal.bind.TypeAdapters.JSON_ELEMENT
	 * <li> and com.google.gson.internal.bind.ObjectTypeAdapter
	 * </ul>
	 */
	@SerializedName("null_value")
	private JsonElement nullValue;

	/*
	 * Text datatype
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/text.html
	 */

	private String analyzer;

	/*
	 * Text datatype
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/search-analyzer.html
	 */

	@SerializedName("search_analyzer")
	private String searchAnalyzer;

	/*
	 * Keyword datatype
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/keyword.html
	 */

	private String normalizer;

	/*
	 * Date datatype
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/date.html
	 */
	private List<String> format;

	@SerializedName("scaling_factor")
	private Double scalingFactor;

	@SerializedName("term_vector")
	private String termVector;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<String> getFormat() {
		return format;
	}

	public void setFormat(List<String> format) {
		this.format = format;
	}

	public Boolean getIndex() {
		return index;
	}

	public void setIndex(Boolean index) {
		this.index = index;
	}

	public Boolean getNorms() {
		return norms;
	}

	public void setNorms(Boolean norms) {
		this.norms = norms;
	}

	public Boolean getDocValues() {
		return docValues;
	}

	public void setDocValues(Boolean docValues) {
		this.docValues = docValues;
	}

	public Boolean getStore() {
		return store;
	}

	public void setStore(Boolean store) {
		this.store = store;
	}

	public JsonElement getNullValue() {
		return nullValue;
	}

	public void setNullValue(JsonElement nullValue) {
		this.nullValue = nullValue;
	}

	public String getAnalyzer() {
		return analyzer;
	}

	public void setAnalyzer(String analyzer) {
		this.analyzer = analyzer;
	}

	public String getSearchAnalyzer() {
		return searchAnalyzer;
	}

	public void setSearchAnalyzer(String searchAnalyzer) {
		this.searchAnalyzer = searchAnalyzer;
	}

	public String getNormalizer() {
		return normalizer;
	}

	public void setNormalizer(String normalizer) {
		this.normalizer = normalizer;
	}

	public Double getScalingFactor() {
		return scalingFactor;
	}

	public void setScalingFactor(Double scalingFactor) {
		this.scalingFactor = scalingFactor;
	}

	public String getTermVector() {
		return termVector;
	}

	public void setTermVector(String termVector) {
		this.termVector = termVector;
	}
}
