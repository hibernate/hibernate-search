/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.Iterator;
import java.util.List;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.spi.HighlightProjectionBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.AbstractStubSearchQueryElementFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexNodeContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;

public class StubFieldHighlightProjection extends StubSearchProjection<List<String>> {
	private final String fieldPath;
	private final String highlighterName;

	public StubFieldHighlightProjection(String fieldPath, String highlighterName) {
		this.fieldPath = fieldPath;
		this.highlighterName = highlighterName;
	}

	@Override
	public Object extract(ProjectionHitMapper<?> projectionHitMapper, Iterator<?> projectionFromIndex,
			StubSearchProjectionContext context) {
		return projectionFromIndex.next();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<String> transform(LoadingResult<?> loadingResult, Object extractedData,
			StubSearchProjectionContext context) {
		return (List<String>) extractedData;
	}

	@Override
	protected String typeName() {
		return "highlight";
	}

	@Override
	protected void toNode(StubProjectionNode.Builder self) {
		self.attribute( "fieldPath", fieldPath );
		self.attribute( "highlighterName", highlighterName );
	}

	public static class Factory extends AbstractStubSearchQueryElementFactory<HighlightProjectionBuilder> {
		@Override
		public HighlightProjectionBuilder create(StubSearchIndexScope scope,
				StubSearchIndexNodeContext node) {
			return new Builder( node.toValueField().absolutePath() );
		}
	}

	static class Builder extends HighlightProjectionBuilder {
		Builder(String fieldPath) {
			super( fieldPath );
		}

		@Override
		public SearchProjection<List<String>> build() {
			return new StubFieldHighlightProjection( path, highlighterName );
		}
	}
}
