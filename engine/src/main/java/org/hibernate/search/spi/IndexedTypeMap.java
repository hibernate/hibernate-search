/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spi;

import java.util.Map.Entry;

/**
 * This represents a Map but exposes a more restricted set of operations,
 * allowing us to better encapsulate the contract and perform some optimisations.
 *
 * Also improves type-safety as it enforces usage of IndexedTypeIdentifier as keys,
 * something the standard Map contract doesn't help with.
 *
 * In an ideal world, we will expose less methods than what is currently listed:
 * some methods have been introduced as Deprecated since the beginning as the plan
 * is to eventually remove them; they currently exist merely to allow a
 * practical migration of code to the new approach in smaller, testable steps.
 *
 * @author Sanne Grinovero
 */
public interface IndexedTypeMap<V> {

	V get(IndexedTypeIdentifier type);

	Iterable<Entry<IndexedTypeIdentifier, V>> entrySet();

	IndexedTypeSet keySet();

	int size();

	boolean isEmpty();

	Iterable<V> values();

	boolean containsKey(IndexedTypeIdentifier type);

	/**
	 * @deprecated This method will be removed. The implementations will be refactored to become immutable.
	 * @param type The key to be used.
	 * @param typeBinding The value being put in the underlying key/value map.
	 */
	@Deprecated
	void put(IndexedTypeIdentifier type, V typeBinding);

	/**
	 * @deprecated use {@link #get(IndexedTypeIdentifier)}. This method will be removed.
	 * @param legacyPojoClass the Class whose type is to be used as a key.
	 * @return the value mapped to this key.
	 */
	@Deprecated
	V get(Class<?> legacyPojoClass);

	/**
	 * Returns a type identified by its unique name.
	 * This method returns instance from a pool of known types,
	 * and will throw an exception when requesting an unknown type name.
	 * @param entityClassName
	 * @return
	 */
	IndexedTypeIdentifier keyFromName(String entityClassName);

	/**
	 * Returns a type identifier to the passed class.
	 * This method could return a type which is not recognized
	 * as an indexed type by other components: it is not coming
	 * from a pool of known types.
	 * @param clazz
	 * @return
	 */
	IndexedTypeIdentifier keyFromPojoType(Class<?> clazz);

}
