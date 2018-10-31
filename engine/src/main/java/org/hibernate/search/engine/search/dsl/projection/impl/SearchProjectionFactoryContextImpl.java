/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.projection.impl;

import org.hibernate.search.engine.search.dsl.projection.DistanceToFieldProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.DocumentReferenceProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.FieldProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.ObjectProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.ReferenceProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.ScoreProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.common.Contracts;


public class SearchProjectionFactoryContextImpl implements SearchProjectionFactoryContext {

	private final SearchProjectionBuilderFactory factory;

	public SearchProjectionFactoryContextImpl(SearchProjectionBuilderFactory factory) {
		this.factory = factory;
	}

	@Override
	public DocumentReferenceProjectionContext documentReference() {
		return new DocumentReferenceProjectionContextImpl( factory );
	}

	@Override
	public <T> FieldProjectionContext<T> field(String absoluteFieldPath, Class<T> clazz) {
		Contracts.assertNotNull( clazz, "clazz" );

		return new FieldProjectionContextImpl<>( factory, absoluteFieldPath, clazz );
	}

	@Override
	public FieldProjectionContext<Object> field(String absoluteFieldPath) {
		return field( absoluteFieldPath, Object.class );
	}

	@Override
	public ReferenceProjectionContext reference() {
		return new ReferenceProjectionContextImpl( factory );
	}

	@Override
	public ObjectProjectionContext object() {
		return new ObjectProjectionContextImpl( factory );
	}

	@Override
	public ScoreProjectionContext score() {
		return new ScoreProjectionContextImpl( factory );
	}

	@Override
	public DistanceToFieldProjectionContext distance(String absoluteFieldPath, GeoPoint center) {
		Contracts.assertNotNull( center, "center" );

		return new DistanceToFieldProjectionContextImpl( factory, absoluteFieldPath, center );
	}
}
