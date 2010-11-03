/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.configuration;

import java.lang.annotation.ElementType;

import org.apache.lucene.search.DefaultSimilarity;
import org.apache.solr.analysis.GermanStemFilterFactory;
import org.apache.solr.analysis.LowerCaseFilterFactory;
import org.apache.solr.analysis.NGramFilterFactory;
import org.apache.solr.analysis.SnowballPorterFilterFactory;
import org.apache.solr.analysis.StandardTokenizerFactory;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.annotations.FilterCacheModeType;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.builtin.LongBridge;
import org.hibernate.search.cfg.ConcatStringBridge;
import org.hibernate.search.cfg.SearchMapping;

public class ProgrammaticSearchMappingFactory {
		
	@Factory
	public SearchMapping build() {
		SearchMapping mapping = new SearchMapping();

		mapping.fullTextFilterDef("security", SecurityFilterFactory.class).cache(FilterCacheModeType.INSTANCE_ONLY)
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
					.similarity( DefaultSimilarity.class )
					.boost( 2 )
					.property( "addressId", ElementType.FIELD ).documentId().name( "id" )
					.property("lastUpdated", ElementType.FIELD)
						.field().name("last-updated")
								.analyzer("en").store(Store.YES)
								.calendarBridge(Resolution.DAY)
					.property("dateCreated", ElementType.FIELD)
						.field().name("date-created").index(Index.TOKENIZED)
								.analyzer("en").store( Store.YES )
								.dateBridge(Resolution.DAY)
					.property("owner", ElementType.FIELD)
						.field()
					.property( "street1", ElementType.FIELD )
						.field()
						.field().name( "street1_ngram" ).analyzer( "ngram" )
						.field()
							.name( "street1_abridged" )
							.bridge( ConcatStringBridge.class ).param( ConcatStringBridge.SIZE, "4" )
					.property( "street2", ElementType.METHOD )
						.field().name( "idx_street2" ).store( Store.YES ).boost( 2 )
				.entity(ProvidedIdEntry.class).indexed()
						.providedId().name("providedidentry").bridge(LongBridge.class)
						.property("name", ElementType.FIELD)
							.field().name("providedidentry.name").analyzer("en").index(Index.TOKENIZED).store(Store.YES)
						.property("blurb", ElementType.FIELD)
							.field().name("providedidentry.blurb").analyzer("en").index(Index.TOKENIZED).store(Store.YES)
						.property("age", ElementType.FIELD)
							.field().name("providedidentry.age").analyzer("en").index(Index.TOKENIZED).store(Store.YES)
				.entity(ProductCatalog.class).indexed()
					.similarity( DefaultSimilarity.class )
					.boost( 2 )
					.property( "id", ElementType.FIELD ).documentId().name( "id" )
					.property("name", ElementType.FIELD)
						.field().name("productCatalogName").index(Index.TOKENIZED).analyzer("en").store(Store.YES)
					.property("items", ElementType.FIELD)
						.indexEmbedded()
				.entity(Item.class)
					.indexed()
					.property("description", ElementType.FIELD)
						.field().name("description").analyzer("en").index(Index.TOKENIZED).store(Store.YES)
					.property("productCatalog", ElementType.FIELD)
						.containedIn()
					.property("price", ElementType.FIELD)
						.field()
						.numericField().precisionStep(10)
				.entity(DynamicBoostedDescLibrary.class)
					.dynamicBoost(CustomBoostStrategy.class)
					.indexed()
					.property("libraryId", ElementType.FIELD)
						.documentId().name("id")
					.property("name", ElementType.FIELD)
						.dynamicBoost(CustomFieldBoostStrategy.class)
						.field().store(Store.YES)
				.entity(Departments.class)
					.classBridge(CatDeptsFieldsClassBridge.class)
						.name("branchnetwork")
						.index(Index.TOKENIZED)
						.store(Store.YES)
						.param("sepChar", " ")
					.classBridge(EquipmentType.class)
						.name("equiptype")
						.index(Index.TOKENIZED)
						.store(Store.YES)
							.param("C", "Cisco")
							.param("D", "D-Link")
							.param("K", "Kingston")
							.param("3", "3Com")
					  .indexed()
					.property("deptsId", ElementType.FIELD)
						.documentId().name("id")
					.property("branchHead", ElementType.FIELD)
						.field().store(Store.YES)
					.property("network", ElementType.FIELD)
						.field().store(Store.YES)
					.property("branch", ElementType.FIELD)
						.field().store(Store.YES)
					.property("maxEmployees", ElementType.FIELD)
						.field().index(Index.UN_TOKENIZED).store(Store.YES)
				.entity( BlogEntry.class ).indexed()
					.property( "title", ElementType.METHOD )
						.field()
					.property( "description", ElementType.METHOD )
						.field()
					.property( "language", ElementType.METHOD )
						.analyzerDiscriminator(BlogEntry.BlogLangDiscriminator.class)
					.property("dateCreated", ElementType.METHOD)
						.field()
							.name("blog-entry-created")
								.analyzer("en")
								.store(Store.YES)
								.dateBridge(Resolution.DAY);
		return mapping;
	}
}
