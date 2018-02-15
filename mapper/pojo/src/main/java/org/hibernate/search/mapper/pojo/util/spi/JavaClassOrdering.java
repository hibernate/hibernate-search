/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.util.spi;

import java.util.Arrays;
import java.util.stream.Stream;

public final class JavaClassOrdering extends AbstractPojoTypeOrdering<Class<?>> {

	private static final JavaClassOrdering INSTANCE = new JavaClassOrdering();

	public static JavaClassOrdering get() {
		return INSTANCE;
	}

	private JavaClassOrdering() {
	}

	@Override
	protected Class<?> getSuperClass(Class<?> subType) {
		return subType.getSuperclass();
	}

	@Override
	protected Stream<Class<?>> getDeclaredInterfaces(Class<?> subType) {
		return Arrays.stream( subType.getInterfaces() );
	}
}
