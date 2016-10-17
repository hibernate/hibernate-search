/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import org.hibernate.search.analyzer.impl.AnalyzerReference;
import org.hibernate.search.analyzer.impl.RemoteAnalyzerReference;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * An object responsible for storing the context of the current position in
 * the mapping, and for providing access to it (read/write).
 *
 * Used in Elasticsearch mapping generation.
 *
 * @author Yoann Rodiere
 */
final class ElasticsearchMappingBuilder {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private final EntityIndexBinding binding;

	private final ElasticsearchMappingBuilder parent;
	private final TypeMetadata typeMetadata;

	private final PathComponentExtractor pathComponentExtractor;
	private final JsonObject mappingJson;

	public ElasticsearchMappingBuilder(EntityIndexBinding binding, JsonObject mappingJson) {
		this( null, binding, binding.getDocumentBuilder().getTypeMetadata(), mappingJson, new PathComponentExtractor() );
	}

	private ElasticsearchMappingBuilder(ElasticsearchMappingBuilder parent, EntityIndexBinding binding, TypeMetadata typeMetadata,
			JsonObject mappingJson, PathComponentExtractor pathComponentExtractor) {
		this.binding = binding;
		this.parent = parent;
		this.mappingJson = mappingJson;
		this.typeMetadata = typeMetadata;
		this.pathComponentExtractor = pathComponentExtractor;
	}

	public ElasticsearchMappingBuilder createEmbedded(EmbeddedTypeMetadata embeddedTypeMetadata) {
		PathComponentExtractor extractor = pathComponentExtractor.clone();
		extractor.append( embeddedTypeMetadata.getEmbeddedFieldPrefix() );

		JsonObject currentMappingJson = mappingJson;
		String newPathComponent = extractor.next();
		while ( newPathComponent != null ) {
			/*
			 * The property can already exist if we have both a @Field and @IndexedEmbedded
			 * with the same name, but most of the time it should not.
			 * See HSEARCH-2419 for the reason why @Field + @IndexedEmbedded happens.
			 */
			JsonObject newProperty = getPropertyRelative( mappingJson, newPathComponent );

			if ( newProperty == null ) {
				newProperty = new JsonObject();

				// Must be set to avoid errors when there is no property inside this property
				newProperty.addProperty( "type", ElasticsearchFieldType.OBJECT.getElasticsearchString() );

				// TODO HSEARCH-2263 enable nested mapping as needed:
				// * only needed for embedded *-to-many with more than one field
				// * should use the "nested" datatype (instead of the default "object")
				// * for these, the user should be able to opt out (nested would be the safe default mapping in this
				// case, but they could want to opt out when only ever querying on single fields of the embeddable)

				setPropertyRelative( currentMappingJson, newPathComponent, newProperty );
			}

			currentMappingJson = newProperty;
			newPathComponent = extractor.next();
		}

		return new ElasticsearchMappingBuilder( this, binding, embeddedTypeMetadata, currentMappingJson, extractor );
	}

	private static JsonObject getPropertyRelative(JsonObject parent, String name) {
		JsonObject properties = parent.getAsJsonObject( "properties" );
		if ( properties == null ) {
			return null;
		}
		else {
			return properties.getAsJsonObject( name );
		}
	}

	private static void setPropertyRelative(JsonObject parent, String name, JsonObject property) {
		JsonObject properties = parent.getAsJsonObject( "properties" );
		if ( properties == null ) {
			properties = new JsonObject();
			parent.add( "properties", properties );
		}
		properties.add( name, property );
	}

	public boolean hasPropertyAbsolute(String absolutePath) {
		String relativePath = pathComponentExtractor.makeRelative( absolutePath );
		JsonObject property = getPropertyRelative( mappingJson, relativePath );
		return property != null;
	}

	public void setPropertyAbsolute(String absolutePath, JsonObject property) {
		String relativePath = pathComponentExtractor.makeRelative( absolutePath );
		setPropertyRelative( mappingJson, relativePath, property );
	}

	public TypeMetadata getMetadata() {
		return typeMetadata;
	}

	/**
	 * Get the overall boost of a property, taking the parent type's own boost into account.
	 * @param propertyBoost The boost defined on the property itself (ignoring all parent boost),
	 * or {@code 1.0f} if undefined.
	 * @return The overall boost for this property
	 */
	public float getBoost(Float propertyBoost) {
		float boost = propertyBoost == null ? 1.0f : propertyBoost;
		boost *= typeMetadata.getStaticBoost();

		if ( parent != null ) {
			return parent.getBoost( boost );
		}
		else {
			return boost;
		}
	}

	public String getAnalyzerName(AnalyzerReference analyzerReference, String fieldName) {
		if ( !analyzerReference.is( RemoteAnalyzerReference.class ) ) {
			LOG.analyzerIsNotRemote( getBeanClass(), fieldName, analyzerReference );
			return null;
		}
		return analyzerReference.unwrap( RemoteAnalyzerReference.class ).getAnalyzer().getName( fieldName );
	}

	public Class<?> getBeanClass() {
		return binding.getDocumentBuilder().getBeanClass();
	}

}