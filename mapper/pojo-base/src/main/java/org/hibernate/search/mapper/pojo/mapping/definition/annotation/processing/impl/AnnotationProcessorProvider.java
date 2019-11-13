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
		this.typeAnnotationProcessors = CollectionHelper.toImmutableList( CollectionHelper.asList(
				new IndexedProcessor(),
				new RoutingKeyBridgeProcessor(),
				new TypeBridgeProcessor()
		) );

		this.propertyAnnotationProcessors = CollectionHelper.toImmutableList( CollectionHelper.asList(
				new MarkerProcessor(),
				new AssociationInverseSideProcessor(),
				new IndexingDependencyProcessor(),
				new DocumentIdProcessor(),
				new PropertyBridgeProcessor(),
				new GenericFieldProcessor(),
				new FullTextFieldProcessor(),
				new KeywordFieldProcessor(),
				new ScaledNumberFieldProcessor(),
				new NonStandardFieldProcessor(),
				new IndexedEmbeddedProcessor()
		) );
	}

	public List<TypeAnnotationProcessor<?>> getTypeAnnotationProcessors() {
		return typeAnnotationProcessors;
	}

	public List<PropertyAnnotationProcessor<?>> getPropertyAnnotationProcessors() {
		return propertyAnnotationProcessors;
	}

}
