package org.hibernate.search.genericjpa.factory.impl;

import org.hibernate.search.genericjpa.annotations.InIndex;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by Martin on 02.10.2015.
 */
public class SubClassSupportInstanceInitializerTest {

	@Test
	public void testSubClassSupport() {
		SubClassSupportInstanceInitializer initializer = SubClassSupportInstanceInitializer.INSTANCE;
		SubClass subClassObj = new SubClass();
		assertEquals(
				WithInIndex.class, initializer.getClass( subClassObj )
		);
	}

	@Test
	public void testDefaultBehaviour() {
		SubClassSupportInstanceInitializer initializer = SubClassSupportInstanceInitializer.INSTANCE;
		WithoutInIndex withoutInIndex = new WithoutInIndex();
		assertEquals(
				WithoutInIndex.class, initializer.getClass( withoutInIndex )
		);
	}

	@InIndex
	public static class WithInIndex {

	}

	public static class SubClass extends WithInIndex {

	}

	public static class WithoutInIndex {

	}

}
