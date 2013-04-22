package msi.gama.headless.core;

import msi.gama.kernel.experiment.IExperimentSpecies;
import msi.gama.runtime.exceptions.GamaRuntimeException;

public interface IHeadLessExperiment extends IExperimentSpecies {

	public void start(int nbStep);

	public void setParameterWithName(String name, Object value) throws GamaRuntimeException,
		InterruptedException;

	public Object getParameterWithName(String name) throws GamaRuntimeException,
		InterruptedException;

	public void setSeed(double seed);
}
