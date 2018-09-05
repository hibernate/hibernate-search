/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.query.impl;

import org.hibernate.search.elasticsearch.work.impl.SearchResult;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class EmptySearchResult implements SearchResult {

	private static final EmptySearchResult INSTANCE = new EmptySearchResult();

	public static SearchResult get() {
		return INSTANCE;
	}

	private EmptySearchResult() {
		// Use get()
	}

	@Override
	public JsonArray getHits() {
		return new JsonArray();
	}

	@Override
	public int getTotalHitCount() {
		return 0;
	}

	@Override
	public JsonObject getAggregations() {
		return new JsonObject();
	}

	@Override
	public int getTook() {
		return 0;
	}

	@Override
	public boolean getTimedOut() {
		return false;
	}

	@Override
	public String getScrollId() {
		return null;
	}

}
