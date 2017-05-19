/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl.model;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

/**
 * An object representing Elasticsearch property mappings, i.e. the mappings of properties inside a type mapping.
 *
 * See https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html#mapping-type
 * @author Yoann Rodiere
 */
public class PropertyMapping extends TypeMapping {

	private DataType type;

	/*
	 * Attributes common to multiple datatypes
	 */

	private Float boost;

	private IndexType index;

	private NormsType norms;

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
	private JsonPrimitive nullValue;

	/**
	 * Must be null when we don't want to include it in JSON serialization.
	 */
	private Map<String, PropertyMapping> fields;

	/*
	 * Text datatype
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/text.html
	 */

	private String analyzer;

	@SerializedName("fielddata")
	private FieldDataType fieldData;

	/*
	 * Keyword datatype
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/keyword.html
	 */

	private String normalizer;

	/*
	 * Date datatype
	 * https://www.elastic.co/guide/en/elasticsearch/reference/current/date.html
	 */
	@JsonAdapter(ElasticsearchFormatJsonAdapter.class)
	private List<String> format;

	public DataType getType() {
		return type;
	}

	public void setType(DataType type) {
		this.type = type;
	}

	public List<String> getFormat() {
		return format;
	}

	public void setFormat(List<String> format) {
		this.format = format;
	}

	public Float getBoost() {
		return boost;
	}

	public void setBoost(Float boost) {
		this.boost = boost;
	}

	public IndexType getIndex() {
		return index;
	}

	public void setIndex(IndexType index) {
		this.index = index;
	}

	public NormsType getNorms() {
		return norms;
	}

	public void setNorms(NormsType norms) {
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

	public JsonPrimitive getNullValue() {
		return nullValue;
	}

	public void setNullValue(JsonPrimitive nullValue) {
		this.nullValue = nullValue;
	}

	public Map<String, PropertyMapping> getFields() {
		return fields;
	}

	private Map<String, PropertyMapping> getInitializedFields() {
		if ( fields == null ) {
			fields = new TreeMap<>();
		}
		return fields;
	}

	public void addField(String name, PropertyMapping mapping) {
		getInitializedFields().put(name, mapping);
	}

	public void removeField(String name) {
		getInitializedFields().remove( name );
	}

	public String getAnalyzer() {
		return analyzer;
	}

	public void setAnalyzer(String analyzer) {
		this.analyzer = analyzer;
	}

	public String getNormalizer() {
		return normalizer;
	}

	public void setNormalizer(String normalizer) {
		this.normalizer = normalizer;
	}

	public FieldDataType getFieldData() {
		return fieldData;
	}

	public void setFieldData(FieldDataType fieldData) {
		this.fieldData = fieldData;
	}

	@Override
	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson( this );
	}

}
