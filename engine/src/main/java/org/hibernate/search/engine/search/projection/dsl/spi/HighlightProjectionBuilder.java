/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.spi;

import java.util.List;

import org.hibernate.search.engine.search.projection.SearchProjection;

public abstract class HighlightProjectionBuilder {
	protected final String path;
	protected String highlighterName;

	protected HighlightProjectionBuilder(String path) {
		this.path = path;
	}

	public HighlightProjectionBuilder highlighter(String highlighterName) {
		this.highlighterName = highlighterName;
		return this;
	}

	public abstract SearchProjection<List<String>> build();
}
