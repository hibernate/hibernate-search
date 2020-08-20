/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.util.TokenizerFactory;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.util.StringHelper;

/**
 * @author Emmanuel Bernard
 */
public class FieldMapping {
	private final SearchMapping mapping;
	private final EntityDescriptor entity;
	private final PropertyDescriptor property;
	private final Map<String, Object> field;

	public FieldMapping(PropertyDescriptor property, EntityDescriptor entity, SearchMapping mapping) {
		this.mapping = mapping;
		this.entity = entity;
		this.property = property;
		this.field = new HashMap<String, Object>();
		property.addField( field );
	}

	/**
	 * @deprecated Only provided to avoid breaking the API in {@link NumericFieldMapping}, which extends this class.
	 * Do not extend this class, just re-implement methods.
	 */
	@Deprecated
	FieldMapping(String fieldName, PropertyDescriptor property, EntityDescriptor entity, SearchMapping mapping) {
		this.mapping = mapping;
		this.entity = entity;
		this.property = property;
		Map<String, Object> existingField = getExistingField( property, fieldName );
		if ( existingField != null ) {
			this.field = existingField;
		}
		else {
			this.field = new HashMap<String, Object>();
			property.addField( field );
		}
	}

	private static Map<String, Object> getExistingField(PropertyDescriptor property, String nameOfFieldToGet) {
		if ( nameOfFieldToGet.isEmpty() ) {
			nameOfFieldToGet = property.getName();
		}
		for ( Map<String, Object> field : property.getFields() ) {
			String fieldName = (String) field.get( "name" );
			if ( StringHelper.isEmpty( fieldName ) ) {
				fieldName = property.getName();
			}
			if ( nameOfFieldToGet.equals( fieldName ) ) {
				return field;
			}
		}
		return null;
	}

	public FieldMapping name(String fieldName) {
		field.put( "name", fieldName );
		return this;
	}

	public FieldMapping store(Store store) {
		field.put( "store", store );
		return this;
	}

	public FieldMapping index(Index index) {
		field.put( "index", index );
		return this;
	}

	public FieldMapping analyze(Analyze analyze) {
		field.put( "analyze", analyze );
		return this;
	}

	public FieldMapping norms(Norms norms) {
		field.put( "norms", norms );
		return this;
	}

	public FieldMapping termVector(TermVector termVector) {
		field.put( "termVector", termVector );
		return this;
	}

	public FieldMapping boost(float boost) {
		final Map<String, Object> boostAnn = new HashMap<String, Object>();
		boostAnn.put( "value", boost );
		field.put( "boost", boostAnn );
		return this;
	}

	public FieldBridgeMapping bridge(Class<?> impl) {
		return new FieldBridgeMapping( impl, field, this, property, entity, mapping );
	}

	public FieldMapping analyzer(Class<?> analyzerClass) {
		final Map<String, Object> analyzer = new HashMap<String, Object>();
		analyzer.put( "impl", analyzerClass );
		field.put( "analyzer", analyzer );
		return this;
	}

	public FieldMapping analyzer(String analyzerDef) {
		final Map<String, Object> analyzer = new HashMap<String, Object>();
		analyzer.put( "definition", analyzerDef );
		field.put( "analyzer", analyzer );
		return this;
	}

	public FieldMapping normalizer(Class<?> analyzerClass) {
		final Map<String, Object> normalizer = new HashMap<String, Object>();
		normalizer.put( "impl", analyzerClass );
		field.put( "normalizer", normalizer );
		return this;
	}

	public FieldMapping normalizer(String analyzerDef) {
		final Map<String, Object> normalizer = new HashMap<String, Object>();
		normalizer.put( "definition", analyzerDef );
		field.put( "normalizer", normalizer );
		return this;
	}

	public FieldMapping indexNullAs(String indexNullAs) {
		field.put( "indexNullAs", indexNullAs );
		return this;
	}

	public FieldMapping field() {
		return new FieldMapping( property, entity, mapping );
	}

	public NumericFieldMapping numericField() {
		String forField = (String) field.get( "name" );
		if ( forField == null ) {
			forField = property.getName();
		}

		return new NumericFieldMapping( forField, property, entity, mapping );
	}

	public SortableFieldMapping sortableField() {
		String forField = (String) field.get( "name" );
		if ( forField == null ) {
			forField = property.getName();
		}
		return new SortableFieldMapping( forField, property, entity, mapping );
	}

	public FacetMapping facet() {
		String forField = (String) field.get( "name" );
		if ( forField == null ) {
			forField = property.getName();
		}
		return new FacetMapping( forField, property, entity, mapping );
	}

	public PropertyMapping property(String name, ElementType type) {
		return new PropertyMapping( name, type, entity, mapping );
	}

	public DateBridgeMapping dateBridge(Resolution resolution) {
		return new DateBridgeMapping( mapping, entity, property, resolution );
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return analyzerDef( name, "", tokenizerFactory );
	}

	public AnalyzerDefMapping analyzerDef(String name, String tokenizerName, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping( name, tokenizerName, tokenizerFactory, mapping );
	}

	public NormalizerDefMapping normalizerDef(String name) {
		return new NormalizerDefMapping( name, mapping );
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping( entityType, mapping );
	}

	public CalendarBridgeMapping calendarBridge(Resolution resolution) {
		return new CalendarBridgeMapping( mapping, entity, property, resolution );
	}
}
