/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import java.util.Map;

import org.hibernate.search.analyzer.impl.AnalyzerReference;
import org.hibernate.search.analyzer.impl.RemoteAnalyzerReference;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.schema.impl.model.DataType;
import org.hibernate.search.elasticsearch.schema.impl.model.PropertyMapping;
import org.hibernate.search.elasticsearch.schema.impl.model.TypeMapping;
import org.hibernate.search.elasticsearch.util.impl.PathComponentExtractor;
import org.hibernate.search.elasticsearch.util.impl.PathComponentExtractor.ConsumptionLimit;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.util.logging.impl.LoggerFactory;

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
	private final TypeMapping elasticsearchMapping;

	public ElasticsearchMappingBuilder(EntityIndexBinding binding, TypeMapping elasticsearchMapping) {
		this( null, binding, binding.getDocumentBuilder().getTypeMetadata(), elasticsearchMapping, new PathComponentExtractor() );
	}

	private ElasticsearchMappingBuilder(ElasticsearchMappingBuilder parent, EntityIndexBinding binding, TypeMetadata typeMetadata,
			TypeMapping elasticsearchMapping, PathComponentExtractor pathComponentExtractor) {
		this.binding = binding;
		this.parent = parent;
		this.elasticsearchMapping = elasticsearchMapping;
		this.typeMetadata = typeMetadata;
		this.pathComponentExtractor = pathComponentExtractor;
	}

	public ElasticsearchMappingBuilder createEmbedded(EmbeddedTypeMetadata embeddedTypeMetadata) {
		PathComponentExtractor newExtractor = pathComponentExtractor.clone();
		newExtractor.append( embeddedTypeMetadata.getEmbeddedFieldPrefix() );

		TypeMapping newElasticsearchMapping = getOrCreateParents( newExtractor );

		return new ElasticsearchMappingBuilder( this, binding, embeddedTypeMetadata, newElasticsearchMapping, newExtractor );
	}

	private TypeMapping getOrCreateParents(PathComponentExtractor extractor) {
		TypeMapping currentElasticsearchMapping = elasticsearchMapping;
		String newPathComponent = extractor.next( ConsumptionLimit.SECOND_BUT_LAST );
		while ( newPathComponent != null ) {
			/*
			 * The property can already exist if we have both a @Field and @IndexedEmbedded
			 * with the same name, but most of the time it should not.
			 * See HSEARCH-2419 for the reason why @Field + @IndexedEmbedded happens.
			 */
			PropertyMapping childPropertyMapping = getPropertyRelative( currentElasticsearchMapping, newPathComponent );

			if ( childPropertyMapping == null ) {
				childPropertyMapping = new PropertyMapping();

				// Must be set to avoid errors when there is no property inside this property
				childPropertyMapping.setType( DataType.OBJECT );

				// TODO HSEARCH-2263 enable nested mapping as needed:
				// * only needed for embedded *-to-many with more than one field
				// * should use the "nested" datatype (instead of the default "object")
				// * for these, the user should be able to opt out (nested would be the safe default mapping in this
				// case, but they could want to opt out when only ever querying on single fields of the embeddable)

				currentElasticsearchMapping.addProperty( newPathComponent, childPropertyMapping );
			}

			currentElasticsearchMapping = childPropertyMapping;
			newPathComponent = extractor.next( ConsumptionLimit.SECOND_BUT_LAST );
		}

		return currentElasticsearchMapping;
	}

	private static PropertyMapping getPropertyRelative(TypeMapping parent, String name) {
		Map<String, PropertyMapping> properties = parent.getProperties();
		if ( properties == null ) {
			return null;
		}
		else {
			return properties.get( name );
		}
	}

	public boolean hasPropertyAbsolute(String absolutePath) {
		/*
		 * Handle cases where the field name contains dots (and therefore requires
		 * creating containing properties).
		 */
		PathComponentExtractor newExtractor = this.pathComponentExtractor.clone();
		newExtractor.appendRelativePart( absolutePath );
		TypeMapping parent = getOrCreateParents( newExtractor );
		String propertyName = newExtractor.next( ConsumptionLimit.LAST );
		return getPropertyRelative( parent, propertyName ) != null;
	}

	public void setPropertyAbsolute(String absolutePath, PropertyMapping propertyMapping) {
		/*
		 * Handle cases where the field name contains dots (and therefore requires
		 * creating containing properties).
		 */
		PathComponentExtractor newExtractor = this.pathComponentExtractor.clone();
		newExtractor.appendRelativePart( absolutePath );
		TypeMapping parent = getOrCreateParents( newExtractor );
		String propertyName = newExtractor.next( ConsumptionLimit.LAST );
		parent.addProperty( propertyName, propertyMapping );
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