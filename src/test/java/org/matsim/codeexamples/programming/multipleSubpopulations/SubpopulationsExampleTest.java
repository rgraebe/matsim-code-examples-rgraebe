/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.codeexamples.programming.multipleSubpopulations;

import static org.junit.Assert.*;

import org.junit.Test;
import org.matsim.codeexamples.mobsim.programming.multipleSubpopulations.RunSubpopulationsExample;

/**
 * @author nagel
 *
 */
public class SubpopulationsExampleTest {

	/**
	 * Test method for {@link RunSubpopulationsExample#main(java.lang.String[])}.
	 */
	@SuppressWarnings("static-method")
	@Test
	public final void testMain() {
		try {
			RunSubpopulationsExample.main(null);
		} catch ( Exception ee ) {
			ee.printStackTrace();
			fail( "Got an exception while running subpopulation example: "+ee ) ;
		}
	}

}
