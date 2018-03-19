/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.model;

import org.hibernate.search.engine.backend.document.model.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.Sortable;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingContributor;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingContributor;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.GeoPointBridge;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.LatitudeMarker;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.LongitudeMarker;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingDefinition;
import org.hibernate.search.integrationtest.showcase.library.bridge.BookMediumBridge;
import org.hibernate.search.integrationtest.showcase.library.bridge.ISBNBridge;
import org.hibernate.search.integrationtest.showcase.library.bridge.LibraryServiceBridge;
import org.hibernate.search.integrationtest.showcase.library.bridge.MultiKeywordStringBridge;
import org.hibernate.search.integrationtest.showcase.library.bridge.VideoMediumBridge;

/**
 * A programmatic mapping contributor that reproduces the exact same mapping as the annotations on model types.
 * <p>
 * While such a programmatic mapping would normally make no sense (we could just use the annotation mapping),
 * it is useful for demonstration purposes.
 */
public class ProgrammaticMappingContributor implements HibernateOrmSearchMappingContributor {
	@Override
	public void contribute(HibernateOrmMappingContributor contributor) {
		ProgrammaticMappingDefinition mapping = contributor.programmaticMapping();

		mapping.type( Library.class ).indexed( Library.INDEX )
				.bridge( new GeoPointBridge.Builder().fieldName( "location" ) )
				.property( "id" ).documentId()
				.property( "name" )
						.field().analyzer( "default" )
						.field( "name_sort" ).sortable( Sortable.YES )
				.property( "collectionSize" )
						.field().sortable( Sortable.YES )
				.property( "latitude" ).marker( new LatitudeMarker.Builder() )
				.property( "longitude" ).marker( new LongitudeMarker.Builder() )
				.property( "services" )
						.field().valueBridge( LibraryServiceBridge.class );

		mapping.type( Document.class )
				.property( "id" ).documentId()
				.property( "title" )
						.field().analyzer( "default" )
						.field( "title_sort" ).sortable( Sortable.YES )
				.property( "summary" )
						.field().analyzer( "default" )
				.property( "tags" )
						.bridge(
								new MultiKeywordStringBridge.Builder()
								.fieldName( "tags" )
						)
				.property( "copies" )
						.indexedEmbedded()
								.includePaths( "medium", "library.location", "library.services" )
								.storage( ObjectFieldStorage.NESTED );
		mapping.type( Book.class ).indexed( Book.INDEX )
				.property( "isbn" )
						.field().valueBridge( ISBNBridge.class );
		mapping.type( Video.class ).indexed( Video.INDEX );

		mapping.type( DocumentCopy.class )
				.property( "document" )
						.indexedEmbedded()
				.property( "library" )
						.indexedEmbedded().maxDepth( 1 );
		mapping.type( BookCopy.class )
				.property( "medium" )
						.field().valueBridge( BookMediumBridge.class );
		mapping.type( VideoCopy.class )
				.property( "medium" )
						.field().valueBridge( VideoMediumBridge.class );
	}
}
