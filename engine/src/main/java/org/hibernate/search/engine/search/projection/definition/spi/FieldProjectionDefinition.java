/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.definition.spi;

import java.util.List;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinitionContext;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

@Incubating
public abstract class FieldProjectionDefinition<P, F> extends AbstractProjectionDefinition<P> {

	protected final String fieldPath;
	protected final Class<F> fieldType;
	protected final ValueConvert valueConvert;

	private FieldProjectionDefinition(String fieldPath, Class<F> fieldType, ValueConvert valueConvert) {
		this.fieldPath = fieldPath;
		this.fieldType = fieldType;
		this.valueConvert = valueConvert;
	}

	@Override
	protected String type() {
		return "field";
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		super.appendTo( appender );
		appender.attribute( "fieldPath", fieldPath )
				.attribute( "fieldType", fieldType )
				.attribute( "multi", multi() )
				.attribute( "valueConvert", valueConvert );
	}

	protected abstract boolean multi();

	@Incubating
	public static final class SingleValued<F> extends FieldProjectionDefinition<F, F> {
		public SingleValued(String fieldPath, Class<F> fieldType, ValueConvert valueConvert) {
			super( fieldPath, fieldType, valueConvert );
		}

		@Override
		protected boolean multi() {
			return false;
		}

		@Override
		public SearchProjection<F> create(SearchProjectionFactory<?, ?> factory,
				ProjectionDefinitionContext context) {
			return factory.field( fieldPath, fieldType, valueConvert ).toProjection();
		}
	}

	@Incubating
	public static final class MultiValued<F> extends FieldProjectionDefinition<List<F>, F> {
		public MultiValued(String fieldPath, Class<F> fieldType, ValueConvert valueConvert) {
			super( fieldPath, fieldType, valueConvert );
		}

		@Override
		protected boolean multi() {
			return true;
		}

		@Override
		public SearchProjection<List<F>> create(SearchProjectionFactory<?, ?> factory,
				ProjectionDefinitionContext context) {
			return factory.field( fieldPath, fieldType, valueConvert ).multi().toProjection();
		}
	}
}
