package org.hibernate.search.backend.elasticsearch.lowlevel.query.impl;

import java.util.Collection;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public final class Queries {

	private Queries() {
	}

	public static ObjectNode matchAll() {
		ObjectNode matchAll = new ObjectNode( JsonNodeFactory.instance );
		matchAll.set( "match_all", new ObjectNode( JsonNodeFactory.instance ) );
		return matchAll;
	}

	public static ObjectNode term(String absoluteFieldPath, String value) {
		ObjectNode predicate = new ObjectNode( JsonNodeFactory.instance );
		ObjectNode innerObject = new ObjectNode( JsonNodeFactory.instance );
		predicate.set( "term", innerObject );
		innerObject.put( absoluteFieldPath, value );
		return predicate;
	}

	public static ObjectNode anyTerm(String absoluteFieldPath, Collection<String> values) {
		ObjectNode predicate = new ObjectNode( JsonNodeFactory.instance );
		ObjectNode innerObject = new ObjectNode( JsonNodeFactory.instance );
		predicate.set( "terms", innerObject );
		ArrayNode terms = new ArrayNode( JsonNodeFactory.instance );
		innerObject.set( absoluteFieldPath, terms );
		for ( String value : values ) {
			terms.add( value );
		}
		return predicate;
	}

	public static ObjectNode boolFilter(ObjectNode must, ArrayNode filters) {
		if ( filters == null || filters.isEmpty() ) {
			return must;
		}
		ObjectNode predicate = new ObjectNode( JsonNodeFactory.instance );
		ObjectNode innerObject = new ObjectNode( JsonNodeFactory.instance );
		predicate.set( "bool", innerObject );
		if ( must != null ) {
			innerObject.set( "must", must );
		}
		innerObject.set( "filter", filters );
		return predicate;
	}

	public static ObjectNode boolCombineMust(ObjectNode must, ArrayNode otherMustClauses) {
		if ( otherMustClauses == null || otherMustClauses.isEmpty() ) {
			return must;
		}
		if ( must == null && otherMustClauses.size() == 1 ) {
			return ( (ObjectNode) otherMustClauses.get( 0 ) );
		}
		ObjectNode predicate = new ObjectNode( JsonNodeFactory.instance );
		ObjectNode innerObject = new ObjectNode( JsonNodeFactory.instance );
		predicate.set( "bool", innerObject );
		ArrayNode mustClauses = new ArrayNode( JsonNodeFactory.instance );
		if ( must != null ) {
			mustClauses.add( must );
		}
		mustClauses.addAll( otherMustClauses );
		innerObject.set( "must", mustClauses );
		return predicate;
	}
}
