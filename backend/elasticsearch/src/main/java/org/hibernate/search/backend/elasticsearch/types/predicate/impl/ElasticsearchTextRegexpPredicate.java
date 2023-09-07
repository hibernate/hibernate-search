/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.AbstractElasticsearchSingleFieldPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.RegexpQueryFlag;
import org.hibernate.search.engine.search.predicate.spi.RegexpPredicateBuilder;
import org.hibernate.search.util.common.AssertionFailure;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ElasticsearchTextRegexpPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final JsonObjectAccessor REGEXP_ACCESSOR = JsonAccessor.root().property( "regexp" ).asObject();

	private static final JsonAccessor<JsonElement> VALUE_ACCESSOR = JsonAccessor.root().property( "value" );
	private static final JsonAccessor<String> FLAGS_ACCESSOR = JsonAccessor.root().property( "flags" ).asString();

	private static final String NO_OPTIONAL_OPERATORS_FLAG_MARK = "NONE";

	private final JsonPrimitive pattern;
	private final Set<RegexpQueryFlag> flags;

	public ElasticsearchTextRegexpPredicate(Builder builder) {
		super( builder );
		this.pattern = builder.pattern;
		this.flags = builder.flags;
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject,
			JsonObject innerObject) {
		VALUE_ACCESSOR.set( innerObject, pattern );

		// set no optional flag as default
		FLAGS_ACCESSOR.set( innerObject, toFlagsMask( flags ) );

		JsonObject middleObject = new JsonObject();
		middleObject.add( absoluteFieldPath, innerObject );

		REGEXP_ACCESSOR.set( outerObject, middleObject );
		return outerObject;
	}

	public static class Factory
			extends AbstractElasticsearchValueFieldSearchQueryElementFactory<RegexpPredicateBuilder, String> {
		@Override
		public RegexpPredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<String> field) {
			return new Builder( scope, field );
		}
	}

	private static class Builder extends AbstractBuilder implements RegexpPredicateBuilder {
		private JsonPrimitive pattern;
		private Set<RegexpQueryFlag> flags;

		private Builder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<String> field) {
			super( scope, field );
		}

		@Override
		public void pattern(String pattern) {
			this.pattern = new JsonPrimitive( pattern );
		}

		@Override
		public void flags(Set<RegexpQueryFlag> flags) {
			this.flags = flags.isEmpty() ? Collections.emptySet() : EnumSet.copyOf( flags );
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchTextRegexpPredicate( this );
		}
	}

	private static String toFlagsMask(Set<RegexpQueryFlag> flags) {
		if ( flags == null || flags.isEmpty() ) {
			return NO_OPTIONAL_OPERATORS_FLAG_MARK;
		}
		StringBuilder flagsMask = new StringBuilder();

		for ( RegexpQueryFlag flag : flags ) {
			if ( flagsMask.length() > 0 ) {
				flagsMask.append( "|" );
			}
			flagsMask.append( getFlagName( flag ) );
		}
		return flagsMask.toString();
	}

	/**
	 * @param flag The flag as defined in Hibernate Search.
	 * @return The name of this flag in Elasticsearch (might be different from flag.name()).
	 */
	private static String getFlagName(RegexpQueryFlag flag) {
		switch ( flag ) {
			case INTERVAL:
				return "INTERVAL";
			case INTERSECTION:
				return "INTERSECTION";
			case ANY_STRING:
				return "ANYSTRING";
			default:
				throw new AssertionFailure( "Unexpected flag: " + flag );
		}
	}
}
