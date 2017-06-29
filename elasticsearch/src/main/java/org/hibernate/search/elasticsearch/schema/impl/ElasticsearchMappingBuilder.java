/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import java.util.Map;

import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.schema.impl.model.DataType;
import org.hibernate.search.elasticsearch.schema.impl.model.PropertyMapping;
import org.hibernate.search.elasticsearch.schema.impl.model.TypeMapping;
import org.hibernate.search.elasticsearch.util.impl.ParentPathMismatchException;
import org.hibernate.search.elasticsearch.util.impl.PathComponentExtractor;
import org.hibernate.search.elasticsearch.util.impl.PathComponentExtractor.ConsumptionLimit;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.spi.IndexedTypeIdentifier;
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
	private TypeMapping elasticsearchMapping;

	/**
	 * Create a root mapping builder.
	 * @param binding The Hibernate Search binding to be translated in an Elasticsearch mapping
	 * @param elasticsearchMapping The Elasticsearch mapping on which properties should be added
	 */
	public ElasticsearchMappingBuilder(EntityIndexBinding binding, TypeMapping elasticsearchMapping) {
		this.binding = binding;
		this.parent = null;
		this.typeMetadata = binding.getDocumentBuilder().getTypeMetadata();
		this.pathComponentExtractor = new PathComponentExtractor();
		this.elasticsearchMapping = elasticsearchMapping;
	}

	/**
	 * Create an embedded mapping builder.
	 * @param parent The builder for the mapping on which the new mapping will be added as a property
	 * @param embeddedTypeMetadata The Hibernate Search metadata to be translated in an Elasticsearch mapping
	 */
	public ElasticsearchMappingBuilder(ElasticsearchMappingBuilder parent, EmbeddedTypeMetadata embeddedTypeMetadata) {
		this.binding = parent.binding;
		this.parent = parent;
		this.typeMetadata = embeddedTypeMetadata;
		this.pathComponentExtractor = parent.clonePathExtractor();
		this.pathComponentExtractor.append( embeddedTypeMetadata.getEmbeddedFieldPrefix() );
		this.elasticsearchMapping = null; // Will be lazily initialized
	}


	private TypeMapping getOrCreateParents(PathComponentExtractor extractor) {
		/*
		 * Lazily add the mapping to its parent, because we'd been asked to do so.
		 * This lazy initialization allows users to use
		 * @IndexedEmbedded(prefix = "foo") in conjunction with @Field(name = "foo") on
		 * the same property, provided the @IndexedEmbedded will not add any sub-property.
		 *
		 * This might seem weird (and arguably is), but this is how users tell Hibernate
		 * Search to unwrap a multi-valued property (array, List, Map, ...) to pass each
		 * value to the field bridge instead of simply passing the container itself.
		 */
		if ( this.elasticsearchMapping == null ) {
			this.elasticsearchMapping = parent.getOrCreateParents( pathComponentExtractor );
		}

		TypeMapping currentElasticsearchMapping = this.elasticsearchMapping;
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

			if ( ! DataType.OBJECT.equals( childPropertyMapping.getType() ) ) {
				throw LOG.fieldIsBothCompositeAndConcrete( getTypeIdentifier(), extractor.getLastComponentAbsolutePath() );
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
		return getPropertyAbsolute( absolutePath ) != null;
	}

	public TypeMapping getPropertyAbsolute(String absolutePath) {
		/*
		 * Handle cases where the field name contains dots (and therefore requires
		 * handling parent properties along the path).
		 */
		PathComponentExtractor newExtractor = createPathExtractorForAbsolutePath( absolutePath );
		TypeMapping currentMapping = elasticsearchMapping;
		String pathComponent = newExtractor.next( ConsumptionLimit.LAST );
		while ( currentMapping != null && pathComponent != null ) {
			currentMapping = getPropertyRelative( currentMapping, pathComponent );
			pathComponent = newExtractor.next( ConsumptionLimit.LAST );
		}
		return currentMapping;
	}

	public void setPropertyAbsolute(String absolutePath, PropertyMapping propertyMapping) {
		/*
		 * Handle cases where the field name contains dots (and therefore requires
		 * creating containing properties).
		 */
		PathComponentExtractor newExtractor = createPathExtractorForAbsolutePath( absolutePath );
		TypeMapping parent = getOrCreateParents( newExtractor );
		String propertyName = newExtractor.next( ConsumptionLimit.LAST );

		Map<String, PropertyMapping> parentProperties = parent.getProperties();
		if ( parentProperties != null && parentProperties.containsKey( propertyName ) ) {
			// Report a name conflict
			PropertyMapping conflictingProperty = parentProperties.get( propertyName );
			DataType conflictingPropertyType = conflictingProperty.getType();
			DataType newPropertyType = propertyMapping.getType();
			if ( conflictingPropertyType.isComposite() != newPropertyType.isComposite() ) {
				throw LOG.fieldIsBothCompositeAndConcrete( getTypeIdentifier(), absolutePath );
			}
			/*
			 * Other conflicts are ignored because the users *may* make them work if
			 * fields are defined approximately the same way.
			 */
		}
		parent.addProperty( propertyName, propertyMapping );
	}

	private PathComponentExtractor clonePathExtractor() {
		PathComponentExtractor newExtractor = pathComponentExtractor.clone();
		/*
		 * Ignore the part of the path that hasn't been consumed yet due to the lazy initialization
		 * not having been performed yet.
		 * See getOrCreateParents().
		 */
		newExtractor.flushTo( ConsumptionLimit.SECOND_BUT_LAST );
		return newExtractor;
	}

	private PathComponentExtractor createPathExtractorForAbsolutePath(String absolutePath) {
		try {
			PathComponentExtractor newExtractor = clonePathExtractor();
			newExtractor.appendRelativePart( absolutePath );
			return newExtractor;
		}
		catch (ParentPathMismatchException e) {
			throw LOG.indexedEmbeddedPrefixBypass( getTypeIdentifier(), e.getMismatchingPath(), e.getExpectedParentPath() );
		}
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

	public IndexedTypeIdentifier getTypeIdentifier() {
		return binding.getDocumentBuilder().getTypeIdentifier();
	}

}