/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.models.spi;

import java.util.stream.Stream;

import org.hibernate.models.spi.ClassBasedTypeDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.search.util.common.reflect.spi.AbstractTypeOrdering;

final class PojoModelsClassOrdering extends AbstractTypeOrdering<ClassDetails> {

	private final ClassDetailsRegistry classDetailsRegistry;

	PojoModelsClassOrdering(ClassDetailsRegistry classDetailsRegistry) {
		this.classDetailsRegistry = classDetailsRegistry;
	}

	@Override
	protected ClassDetails superClass(ClassDetails subType) {
		ClassDetails superClass = subType.getSuperClass();
		if ( superClass == null && subType.toJavaClass().isInterface() ) {
			// Make sure Object is considered a superclass of *every* type, even interfaces.
			superClass = classDetailsRegistry.getClassDetails( Object.class.getName() );
		}
		return superClass;
	}

	@Override
	protected Stream<ClassDetails> declaredInterfaces(ClassDetails subType) {
		return subType.getImplementedInterfaces().stream()
				.filter( typeDetails -> typeDetails.getTypeKind().equals( TypeDetails.Kind.CLASS ) )
				.map( TypeDetails::asClassType )
				.map( ClassBasedTypeDetails::getClassDetails );
	}
}
