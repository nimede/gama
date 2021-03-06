/*********************************************************************************************
 * 
 *
 * 'IMoleExperiment.java', in plugin 'msi.gama.headless', is part of the source code of the 
 * GAMA modeling and simulation platform.
 * (c) 2007-2014 UMI 209 UMMISCO IRD/UPMC & Partners
 * 
 * Visit https://code.google.com/p/gama-platform/ for license information and developers contact.
 * 
 * 
 **********************************************************************************************/
package msi.gama.headless.openmole;

import msi.gama.headless.core.IExperiment;

public interface IMoleExperiment extends IExperiment
{
    //keep to ensure compatibility with openMole	
	void play(int finalStep);
	void play(String exp, int finalStep);
}