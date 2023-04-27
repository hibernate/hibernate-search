/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.definition.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.spi.CompositeProjectionDefinition;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionInnerStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.mapper.pojo.reporting.impl.PojoConstructorProjectionDefinitionMessages;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.spi.ToStringTreeAppendable;
import org.hibernate.search.util.common.logging.impl.CommaSeparatedClassesFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueCreateHandle;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

public final class PojoConstructorProjectionDefinition<T>
		implements CompositeProjectionDefinition<T>, ToStringTreeAppendable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private static final PojoConstructorProjectionDefinitionMessages MESSAGES = PojoConstructorProjectionDefinitionMessages.INSTANCE;

	private final ConstructorIdentifier constructor;
	private final ValueCreateHandle<? extends T> valueCreateHandle;
	private final List<InnerProjectionDefinition> innerDefinitions;

	public PojoConstructorProjectionDefinition(PojoConstructorModel<T> constructor,
			List<InnerProjectionDefinition> innerDefinitions) {
		this.constructor = new ConstructorIdentifier( constructor );
		this.valueCreateHandle = constructor.handle();
		this.innerDefinitions = innerDefinitions;
	}

	@Override
	public String toString() {
		return "PojoConstructorProjectionDefinition["
				+ "valueCreateHandle=" + valueCreateHandle
				+ ']';
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "valueCreateHandle", valueCreateHandle )
				.attribute( "innerDefinitions", innerDefinitions );
	}

	@Override
	public CompositeProjectionValueStep<?, T> apply(SearchProjectionFactory<?, ?> projectionFactory,
			CompositeProjectionInnerStep initialStep) {
		int i = -1;
		try {
			SearchProjection<?>[] innerProjections = new SearchProjection<?>[innerDefinitions.size()];
			for ( i = 0; i < innerDefinitions.size(); i++ ) {
				innerProjections[i] = innerDefinitions.get( i ).create( projectionFactory );
			}
			return initialStep.from( innerProjections ).asArray( valueCreateHandle );
		}
		catch (ConstructorProjectionApplicationException e) {
			// We already know what prevented from applying a projection constructor correctly,
			// just add a parent constructor and re-throw:
			ProjectionConstructorPath path = new ProjectionConstructorPath( e.projectionConstructorPath(), i, constructor );
			throw log.errorApplyingProjectionConstructor(
					e.getCause().getMessage(), e, path
			);
		}
		catch (SearchException e) {
			ProjectionConstructorPath path = new ProjectionConstructorPath( constructor );
			throw log.errorApplyingProjectionConstructor( e.getMessage(), e, path );
		}
	}

	public static class ConstructorIdentifier {
		private final String name;
		private final Class<?>[] parametersJavaTypes;

		public ConstructorIdentifier(PojoConstructorModel<?> constructor) {
			this.name = constructor.typeModel().name();
			this.parametersJavaTypes = constructor.parametersJavaTypes();
		}

		public String toHighlightedString(int position) {
			return name + "(" + CommaSeparatedClassesFormatter.formatHighlighted( parametersJavaTypes, position ) + ")";
		}

		@Override
		public String toString() {
			return toHighlightedString( -1 );
		}
	}

	public static class ProjectionConstructorPath {
		private final ProjectionConstructorPath child;
		private final int position;
		private final ConstructorIdentifier constructor;

		public ProjectionConstructorPath(ProjectionConstructorPath child, int position,
				ConstructorIdentifier constructor) {
			this.child = child;
			this.position = position;
			this.constructor = constructor;
		}

		public ProjectionConstructorPath(ConstructorIdentifier constructor) {
			this( null, -1, constructor );
		}

		public String toPrefixedString() {
			return "\n" + MESSAGES.executedConstructorPath() + "\n" + this;
		}

		@Override
		public String toString() {
			return child == null ? constructor.toString() :
					child + "\n\t\u2937 for parameter #" + position + " in " + constructor.toHighlightedString(
							position );
		}
	}
}
