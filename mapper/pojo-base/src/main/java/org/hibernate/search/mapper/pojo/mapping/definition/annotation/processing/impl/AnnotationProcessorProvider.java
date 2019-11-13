/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.util.List;

import org.hibernate.search.util.common.impl.CollectionHelper;

public class AnnotationProcessorProvider {

	private final List<TypeAnnotationProcessor<?>> typeAnnotationProcessors;
	private final List<PropertyAnnotationProcessor<?>> propertyAnnotationProcessors;

	public AnnotationProcessorProvider() {
		AnnotationProcessorHelper helper = new AnnotationProcessorHelper();

		this.typeAnnotationProcessors = CollectionHelper.toImmutableList( CollectionHelper.asList(
				new IndexedProcessor( helper ),
				new RoutingKeyBridgeProcessor( helper ),
				new TypeBridgeProcessor( helper )
		) );

		this.propertyAnnotationProcessors = CollectionHelper.toImmutableList( CollectionHelper.asList(
				new MarkerProcessor( helper ),
				new AssociationInverseSideProcessor( helper ),
				new IndexingDependencyProcessor( helper ),
				new DocumentIdProcessor( helper ),
				new PropertyBridgeProcessor( helper ),
				new GenericFieldProcessor( helper ),
				new FullTextFieldProcessor( helper ),
				new KeywordFieldProcessor( helper ),
				new ScaledNumberFieldProcessor( helper ),
				new NonStandardFieldProcessor( helper ),
				new IndexedEmbeddedProcessor( helper )
		) );
	}

	public List<TypeAnnotationProcessor<?>> getTypeAnnotationProcessors() {
		return typeAnnotationProcessors;
	}

	public List<PropertyAnnotationProcessor<?>> getPropertyAnnotationProcessors() {
		return propertyAnnotationProcessors;
	}

}
