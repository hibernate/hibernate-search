/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.hcann.spi;

import java.util.Arrays;
import java.util.stream.Stream;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.search.mapper.pojo.util.spi.AbstractPojoTypeOrdering;

final class PojoXClassOrdering extends AbstractPojoTypeOrdering<XClass> {

	private static final PojoXClassOrdering INSTANCE = new PojoXClassOrdering();

	public static PojoXClassOrdering get() {
		return INSTANCE;
	}

	private PojoXClassOrdering() {
	}

	@Override
	protected XClass getSuperClass(XClass subType) {
		return subType.getSuperclass();
	}

	@Override
	protected Stream<XClass> getDeclaredInterfaces(XClass subType) {
		return Arrays.stream( subType.getInterfaces() );
	}
}
