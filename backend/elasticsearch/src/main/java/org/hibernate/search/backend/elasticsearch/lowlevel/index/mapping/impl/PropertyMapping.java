/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

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
 * CAUTION:
 * 1. JSON serialization is controlled by a specific adapter, which must be
 * updated whenever fields of this class are added, renamed or removed.
 *
 * 2. Whenever adding more properties consider adding property validation to PropertyMappingValidator.
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

	/*
	 * Dense vector datatype
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/dense-vector.html
	 */
	@SerializedName("element_type")
	private String elementType;

	/*
	 * Dense vector datatype
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/dense-vector.html
	 */
	private Integer dims;

	/*
	 * Dense vector datatype
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/dense-vector.html
	 */
	private String similarity;

	/*
	 * Dense vector datatype
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/dense-vector.html
	 */
	@SerializedName("index_options")
	private ElasticsearchDenseVectorIndexOptions indexOptions;

	/*
	 * k-NN vector datatype
	 * https://opensearch.org/docs/latest/field-types/supported-field-types/knn-vector/
	 */
	private Integer dimension;

	/*
	 * k-NN vector datatype
	 * https://opensearch.org/docs/latest/field-types/supported-field-types/knn-vector/
	 */
	private OpenSearchVectorTypeMethod method;

	/*
	 * k-NN vector datatype
	 * https://opensearch.org/docs/latest/field-types/supported-field-types/knn-vector/
	 */
	@SerializedName("data_type")
	private String dataType;


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

	public String getElementType() {
		return elementType;
	}

	public void setElementType(String elementType) {
		this.elementType = elementType;
	}

	public Integer getDims() {
		return dims;
	}

	public void setDims(Integer dims) {
		this.dims = dims;
	}

	public String getSimilarity() {
		return similarity;
	}

	public void setSimilarity(String similarity) {
		this.similarity = similarity;
	}

	public ElasticsearchDenseVectorIndexOptions getIndexOptions() {
		return indexOptions;
	}

	public void setIndexOptions(ElasticsearchDenseVectorIndexOptions indexOptions) {
		this.indexOptions = indexOptions;
	}

	public Integer getDimension() {
		return dimension;
	}

	public void setDimension(Integer dimension) {
		this.dimension = dimension;
	}

	public OpenSearchVectorTypeMethod getMethod() {
		return method;
	}

	public void setMethod(OpenSearchVectorTypeMethod method) {
		this.method = method;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}
}
