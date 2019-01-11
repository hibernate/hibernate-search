/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.fullstack.library.model;

import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.integrationtest.fullstack.library.analysis.LibraryAnalysisConfigurer;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingDefinitionContainerContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.GeoPointBridgeBuilder;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingDefinitionContext;
import org.hibernate.search.integrationtest.fullstack.library.bridge.AccountBorrowalSummaryBridge;
import org.hibernate.search.integrationtest.fullstack.library.bridge.ISBNBridge;
import org.hibernate.search.integrationtest.fullstack.library.bridge.MultiKeywordStringBridge;

/**
 * A programmatic mapping configurer that reproduces the exact same mapping as the annotations on model types.
 * <p>
 * While such a programmatic mapping would normally make no sense (we could just use the annotation mapping),
 * it is useful for demonstration purposes.
 */
public class ProgrammaticMappingConfigurer implements HibernateOrmSearchMappingConfigurer {
	@Override
	public void configure(HibernateOrmMappingDefinitionContainerContext context) {
		ProgrammaticMappingDefinitionContext mapping = context.programmaticMapping();

		mapping.type( Library.class ).indexed( Library.INDEX )
				.bridge( GeoPointBridgeBuilder.forType().fieldName( "location" ) )
				.property( "id" ).documentId()
				.property( "name" )
						.fullTextField().analyzer( LibraryAnalysisConfigurer.ANALYZER_DEFAULT )
						.keywordField( "name_sort" )
								.normalizer( LibraryAnalysisConfigurer.NORMALIZER_SORT )
								.sortable( Sortable.YES )
				.property( "collectionSize" )
						.genericField().sortable( Sortable.YES )
				.property( "latitude" ).marker( GeoPointBridgeBuilder.latitude() )
				.property( "longitude" ).marker( GeoPointBridgeBuilder.longitude() )
				.property( "services" )
						.genericField();

		mapping.type( Document.class )
				.property( "id" ).documentId()
				.property( "title" )
						.fullTextField().analyzer( LibraryAnalysisConfigurer.ANALYZER_DEFAULT )
						.keywordField( "title_sort" )
								.normalizer( LibraryAnalysisConfigurer.NORMALIZER_SORT )
								.sortable( Sortable.YES )
				.property( "summary" )
						.fullTextField().analyzer( LibraryAnalysisConfigurer.ANALYZER_DEFAULT )
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
						.genericField().valueBridge( ISBNBridge.class );
		mapping.type( Video.class ).indexed( Video.INDEX );

		mapping.type( DocumentCopy.class )
				.property( "document" )
						.indexedEmbedded()
				.property( "library" )
						.indexedEmbedded().maxDepth( 1 );
		mapping.type( BookCopy.class )
				.property( "medium" )
						.genericField();
		mapping.type( VideoCopy.class )
				.property( "medium" )
						.genericField();

		mapping.type( Person.class ).indexed( Person.INDEX )
				.property( "firstName" )
						.fullTextField().analyzer( LibraryAnalysisConfigurer.ANALYZER_DEFAULT )
						.keywordField( "firstName_sort" )
								.normalizer( LibraryAnalysisConfigurer.NORMALIZER_SORT )
								.sortable( Sortable.YES )
				.property( "lastName" )
						.fullTextField().analyzer( LibraryAnalysisConfigurer.ANALYZER_DEFAULT )
						.keywordField( "lastName_sort" )
								.normalizer( LibraryAnalysisConfigurer.NORMALIZER_SORT )
								.sortable( Sortable.YES )
				.property( "account" )
						.indexedEmbedded();

		mapping.type( Account.class )
				.bridge( AccountBorrowalSummaryBridge.class );
	}
}
