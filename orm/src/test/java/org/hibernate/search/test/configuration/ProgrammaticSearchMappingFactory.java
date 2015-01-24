/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.lang.annotation.ElementType;

import org.apache.lucene.analysis.de.GermanStemFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.ngram.NGramFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.annotations.FilterCacheModeType;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.SpatialMode;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.builtin.LongBridge;
import org.hibernate.search.cfg.ConcatStringBridge;
import org.hibernate.search.cfg.SearchMapping;

public class ProgrammaticSearchMappingFactory {

	@Factory
	public SearchMapping build() {
		SearchMapping mapping = new SearchMapping();

		mapping.fullTextFilterDef( "security", SecurityFilterFactory.class ).cache( FilterCacheModeType.INSTANCE_ONLY )
				.analyzerDef( "ngram", StandardTokenizerFactory.class )
					.filter( LowerCaseFilterFactory.class )
					.filter( NGramFilterFactory.class )
						.param( "minGramSize", "3" )
						.param( "maxGramSize", "3" )
				.analyzerDef( "english", StandardTokenizerFactory.class )
					.filter( LowerCaseFilterFactory.class )
					.filter( SnowballPorterFilterFactory.class )
				.analyzerDef( "deutsch", StandardTokenizerFactory.class )
					.filter( LowerCaseFilterFactory.class )
					.filter( GermanStemFilterFactory.class )
				.entity( Address.class )
					.indexed()
					.boost( 2 )
					.classBridge( AddressClassBridge.class )
					.analyzer( "english" )
					.property( "addressId", ElementType.FIELD ).documentId().name( "id" )
					.property( "lastUpdated", ElementType.FIELD )
						.field().name( "last-updated" )
								.analyzer( "en" ).store( Store.YES )
								.calendarBridge( Resolution.DAY )
					.property( "dateCreated", ElementType.FIELD )
						.field().name( "date-created" ).index( Index.YES )
								.store( Store.YES )
								.dateBridge( Resolution.DAY )
					.property( "owner", ElementType.FIELD )
						.field()
					.property( "street1", ElementType.FIELD )
						.field()
						.field().name( "street1_ngram" ).analyzer( "ngram" )
						.field()
							.name( "street1_abridged" )
							.bridge( ConcatStringBridge.class ).param( ConcatStringBridge.SIZE, "4" )
					.property( "street2", ElementType.METHOD )
						.field().name( "idx_street2" ).store( Store.YES ).boost( 2 )
				.entity( ProvidedIdEntry.class ).indexed()
						.providedId().name( "providedidentry" ).bridge( LongBridge.class )
						.property( "name", ElementType.FIELD )
							.field().name( "providedidentry.name" ).analyzer( "en" ).index( Index.YES ).store( Store.YES )
						.property( "blurb", ElementType.FIELD )
							.field().name( "providedidentry.blurb" ).analyzer( "en" ).index( Index.YES ).store( Store.YES )
						.property( "age", ElementType.FIELD )
							.field().name( "providedidentry.age" ).analyzer( "en" ).index( Index.YES ).store( Store.YES )
				.entity( ProductCatalog.class ).indexed()
					.boost( 2 )
					.property( "id", ElementType.FIELD ).documentId().name( "id" )
					.property( "name", ElementType.FIELD )
						.field().name( "productCatalogName" ).index( Index.YES ).analyzer( "en" ).store( Store.YES )
					.property( "items", ElementType.FIELD )
						.indexEmbedded()
				.entity( Item.class )
					.indexed()
					.property( "description", ElementType.FIELD )
						.field().name( "description" ).analyzer( "en" ).index( Index.YES ).store( Store.YES )
					.property( "productCatalog", ElementType.FIELD )
						.containedIn()
					.property( "price", ElementType.FIELD )
						.field()
						.numericField().precisionStep( 10 )
				.entity( DynamicBoostedDescLibrary.class )
					.dynamicBoost( CustomBoostStrategy.class )
					.indexed()
					.property( "libraryId", ElementType.FIELD )
						.documentId().name( "id" )
					.property( "name", ElementType.FIELD )
						.dynamicBoost( CustomFieldBoostStrategy.class )
						.field().store( Store.YES )
				.entity( Departments.class )
					.classBridge( CatDeptsFieldsClassBridge.class )
						.name( "branchnetwork" )
						.index( Index.YES )
						.store( Store.YES )
						.param( "sepChar", " " )
					.classBridge( EquipmentType.class )
						.name( "equiptype" )
						.index( Index.YES )
						.store( Store.YES )
							.param( "C", "Cisco" )
							.param( "D", "D-Link" )
							.param( "K", "Kingston" )
							.param( "3", "3Com" )
					.indexed()
					.property( "deptsId", ElementType.FIELD )
						.documentId().name( "id" )
					.property( "branchHead", ElementType.FIELD )
						.field().store( Store.YES )
					.property( "network", ElementType.FIELD )
						.field().store( Store.YES )
					.property( "branch", ElementType.FIELD )
						.field().store( Store.YES )
					.property( "maxEmployees", ElementType.FIELD )
						.field().index( Index.YES ).analyze( Analyze.YES ).store( Store.YES )
				.entity( BlogEntry.class ).indexed()
					.property( "title", ElementType.METHOD )
						.field()
					.property( "description", ElementType.METHOD )
						.field()
					.property( "language", ElementType.METHOD )
						.analyzerDiscriminator( BlogEntry.BlogLangDiscriminator.class )
					.property( "dateCreated", ElementType.METHOD )
						.field()
							.name( "blog-entry-created" )
								.store( Store.YES )
								.dateBridge( Resolution.DAY )
				.entity( MemberLevelTestPoI.class ).indexed()
					.property( "name", ElementType.METHOD )
						.field()
					.property( "location", ElementType.METHOD )
						.spatial().spatialMode( SpatialMode.HASH )
				.entity( ClassLevelTestPoI.class ).indexed()
					.spatial().name( "location" ).spatialMode( SpatialMode.HASH )
					.property( "name", ElementType.METHOD )
						.field()
				.entity( LatLongAnnTestPoi.class ).indexed()
					.spatial().name( "location" )
					.property( "latitude", ElementType.FIELD )
						.latitude().name( "location" )
					.property( "longitude", ElementType.FIELD )
						.longitude().name( "location" )
				.entity( OrderLine.class ).indexed()
					.classBridgeInstance( new OrderLineClassBridge( "orderLineName" ) )
					.classBridgeInstance( new OrderLineClassBridge( null ) )
						.name( "orderLineName_ngram" )
						.analyzer( "ngram" )
					.classBridgeInstance( new OrderLineClassBridge( null ) )
						.param( "fieldName", "orderLineNameViaParam" )
		;

		return mapping;
	}
}
