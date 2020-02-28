/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hibernate.search.engine.search.sort.dsl;

import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Waldemar Kłaczyński
 */
public class FieldSortOptionsStepTest extends EasyMockSupport {

	public FieldSortOptionsStepTest() {
	}

	@BeforeClass
	public static void setUpClass() {
	}

	@AfterClass
	public static void tearDownClass() {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	/**
	 * Test of missing method, of class FieldSortOptionsStep.
	 */
	@Test
	public void testMissing() {
		System.out.println( "missing" );

	}

	/**
	 * Test of multi method, of class FieldSortOptionsStep.
	 */
	@Test
	public void testMulti() {
		System.out.println( "multi" );

//                SearchSession session = Search.session(em);
//                SearchScope<Assortment> scope = session.scope(Assortment.class);
//
//                SearchSortFactory sorter = null;
//                CompositeSortComponentsStep sort = sorter.composite();
//
//                sort.add(sorter.field("prices.bruttoPrice_sort")
//                        .asc()
//                        .multi().sum());
//
//                SearchQuery query = session.search(scope)
//                        .where((f) -> {
//                            return f.matchAll();
//                        })
//                        .sort(sort.toSort()).toQuery();
//		
	}

}
