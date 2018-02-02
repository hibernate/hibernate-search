/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.model.ObjectFieldStorage;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchFieldFormatter;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.impl.ElasticsearchSingleSearchPredicateCollector;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.util.spi.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
// TODO have one version of the factory per dialect, if necessary
public class SearchPredicateFactoryImpl implements ElasticsearchSearchPredicateFactory {

	private static final Log log = LoggerFactory.make( Log.class );

	private static final Gson GSON = new GsonBuilder().create();

	private final Collection<ElasticsearchIndexModel> indexModels;

	public SearchPredicateFactoryImpl(Collection<ElasticsearchIndexModel> indexModels) {
		this.indexModels = indexModels;
	}

	@Override
	public SearchPredicate toSearchPredicate(SearchPredicateContributor<ElasticsearchSearchPredicateCollector> contributor) {
		ElasticsearchSingleSearchPredicateCollector collector = new ElasticsearchSingleSearchPredicateCollector();
		contributor.contribute( collector );
		return new ElasticsearchSearchPredicate( collector.toJson() );
	}

	@Override
	public SearchPredicateContributor<ElasticsearchSearchPredicateCollector> toContributor(SearchPredicate predicate) {
		if ( !( predicate instanceof ElasticsearchSearchPredicate ) ) {
			throw log.cannotMixElasticsearchSearchQueryWithOtherPredicates( predicate );
		}
		return (ElasticsearchSearchPredicate) predicate;
	}

	@Override
	public BooleanJunctionPredicateBuilder<ElasticsearchSearchPredicateCollector> bool() {
		return new BooleanJunctionPredicateBuilderImpl();
	}

	@Override
	public MatchPredicateBuilder<ElasticsearchSearchPredicateCollector> match(String absoluteFieldPath) {
		return new MatchPredicateBuilderImpl( absoluteFieldPath, getFormatter( absoluteFieldPath ) );
	}

	@Override
	public RangePredicateBuilder<ElasticsearchSearchPredicateCollector> range(String absoluteFieldPath) {
		return new RangePredicateBuilderImpl( absoluteFieldPath, getFormatter( absoluteFieldPath ) );
	}

	@Override
	public NestedPredicateBuilder<ElasticsearchSearchPredicateCollector> nested(String absoluteFieldPath) {
		checkNestedField( absoluteFieldPath );
		return new NestedPredicateBuilderImpl( absoluteFieldPath );
	}

	@Override
	public SearchPredicateContributor<ElasticsearchSearchPredicateCollector> fromJsonString(String jsonString) {
		return new UserProvidedJsonPredicateContributor( GSON.fromJson( jsonString, JsonObject.class ) );
	}

	private ElasticsearchFieldFormatter getFormatter(String absoluteFieldPath) {
		ElasticsearchIndexModel indexModelForSelectedFormatter = null;
		ElasticsearchFieldFormatter selectedFormatter = null;
		for ( ElasticsearchIndexModel indexModel : indexModels ) {
			ElasticsearchIndexSchemaFieldNode schemaNode = indexModel.getFieldNode( absoluteFieldPath );
			if ( schemaNode != null ) {
				ElasticsearchFieldFormatter formatter = schemaNode.getFormatter();
				if ( selectedFormatter == null ) {
					selectedFormatter = formatter;
					indexModelForSelectedFormatter = indexModel;
				}
				else if ( !selectedFormatter.equals( formatter ) ) {
					throw log.conflictingFieldFormattersForSearch(
							absoluteFieldPath,
							selectedFormatter, indexModelForSelectedFormatter.getIndexName(),
							formatter, indexModel.getIndexName()
							);
				}
			}
		}
		if ( selectedFormatter == null ) {
			throw log.unknownFieldForSearch( absoluteFieldPath, getIndexNames() );
		}
		return selectedFormatter;
	}

	private void checkNestedField(String absoluteFieldPath) {
		boolean found = false;

		for ( ElasticsearchIndexModel indexModel : indexModels ) {
			ElasticsearchIndexSchemaObjectNode schemaNode = indexModel.getObjectNode( absoluteFieldPath );
			if ( schemaNode != null ) {
				found = true;
				if ( !ObjectFieldStorage.NESTED.equals( schemaNode.getStorage() ) ) {
					throw log.nonNestedFieldForNestedQuery( indexModel.getIndexName(), absoluteFieldPath );
				}
			}
		}
		if ( !found ) {
			for ( ElasticsearchIndexModel indexModel : indexModels ) {
				ElasticsearchIndexSchemaFieldNode schemaNode = indexModel.getFieldNode( absoluteFieldPath );
				if ( schemaNode != null ) {
					throw log.nonObjectFieldForNestedQuery( indexModel.getIndexName(), absoluteFieldPath );
				}
			}
			throw log.unknownFieldForSearch( absoluteFieldPath, getIndexNames() );
		}
	}

	private List<String> getIndexNames() {
		return indexModels.stream().map( ElasticsearchIndexModel::getIndexName ).collect( Collectors.toList() );
	}

}
