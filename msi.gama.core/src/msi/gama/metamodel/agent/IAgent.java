/*
 * GAMA - V1.4 http://gama-platform.googlecode.com
 * 
 * (c) 2007-2011 UMI 209 UMMISCO IRD/UPMC & Partners (see below)
 * 
 * Developers :
 * 
 * - Alexis Drogoul, UMI 209 UMMISCO, IRD/UPMC (Kernel, Metamodel, GAML), 2007-2012
 * - Vo Duc An, UMI 209 UMMISCO, IRD/UPMC (SWT, multi-level architecture), 2008-2012
 * - Patrick Taillandier, UMR 6228 IDEES, CNRS/Univ. Rouen (Batch, GeoTools & JTS), 2009-2012
 * - Beno�t Gaudou, UMR 5505 IRIT, CNRS/Univ. Toulouse 1 (Documentation, Tests), 2010-2012
 * - Phan Huy Cuong, DREAM team, Univ. Can Tho (XText-based GAML), 2012
 * - Pierrick Koch, UMI 209 UMMISCO, IRD/UPMC (XText-based GAML), 2010-2011
 * - Romain Lavaud, UMI 209 UMMISCO, IRD/UPMC (RCP environment), 2010
 * - Francois Sempe, UMI 209 UMMISCO, IRD/UPMC (EMF model, Batch), 2007-2009
 * - Edouard Amouroux, UMI 209 UMMISCO, IRD/UPMC (C++ initial porting), 2007-2008
 * - Chu Thanh Quang, UMI 209 UMMISCO, IRD/UPMC (OpenMap integration), 2007-2008
 */
package msi.gama.metamodel.agent;

import java.util.*;
import msi.gama.common.interfaces.*;
import msi.gama.kernel.experiment.IExperimentAgent;
import msi.gama.kernel.model.IModel;
import msi.gama.kernel.simulation.*;
import msi.gama.metamodel.population.IPopulation;
import msi.gama.metamodel.shape.*;
import msi.gama.metamodel.topology.ITopology;
import msi.gama.precompiler.GamlAnnotations.getter;
import msi.gama.precompiler.GamlAnnotations.setter;
import msi.gama.precompiler.GamlAnnotations.var;
import msi.gama.precompiler.GamlAnnotations.vars;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.IList;
import msi.gaml.skills.ISkill;
import msi.gaml.species.ISpecies;
import msi.gaml.types.IType;

/**
 * Written by drogoul on Apr. 07, Modified on 24 oct. 2010, 05 Apr. 2013
 * 
 * @todo Description
 * 
 */
@vars({ @var(name = IKeyword.NAME, type = IType.STRING), @var(name = IKeyword.MEMBERS, type = IType.LIST),
	@var(name = IKeyword.PEERS, type = IType.LIST), @var(name = IKeyword.AGENTS, type = IType.LIST, of = IType.AGENT),
	@var(name = IKeyword.HOST, type = IType.AGENT),
	@var(name = IKeyword.LOCATION, type = IType.POINT, depends_on = IKeyword.SHAPE),
	@var(name = IKeyword.SHAPE, type = IType.GEOMETRY) })
public interface IAgent extends ISkill, IShape, INamed, Comparable<IAgent>, IStepable {

	/**
	 * Returns all the agents which consider this agent as direct host.
	 * 
	 * @return
	 */
	@getter(IKeyword.MEMBERS)
	public abstract IList<IAgent> getMembers();

	/**
	 * Returns the topology which manages this agent.
	 * 
	 * @return
	 */
	// @getter(IKeyword.TOPOLOGY)
	public abstract ITopology getTopology();

	@setter(IKeyword.MEMBERS)
	public abstract void setMembers(IList<IAgent> members);

	@setter(IKeyword.AGENTS)
	public abstract void setAgents(IList<IAgent> agents);

	/**
	 * Returns all the agents which consider this agent as direct or in-direct host.
	 * 
	 * @return
	 */
	@getter(IKeyword.AGENTS)
	public abstract IList<IAgent> getAgents();

	@setter(IKeyword.PEERS)
	public abstract void setPeers(IList<IAgent> peers);

	/**
	 * Returns agents having the same species and sharing the same direct host with this agent.
	 * 
	 * @return
	 */
	@getter(IKeyword.PEERS)
	public abstract IList<IAgent> getPeers() throws GamaRuntimeException;

	@Override
	@getter(IKeyword.NAME)
	public abstract String getName();

	@Override
	@setter(IKeyword.NAME)
	public abstract void setName(String name);

	@Override
	@getter(value = IKeyword.LOCATION, initializer = true)
	public ILocation getLocation();

	@Override
	@setter(IKeyword.LOCATION)
	public void setLocation(final ILocation l);

	@Override
	@getter(IKeyword.SHAPE)
	public IShape getGeometry();

	@Override
	@setter(IKeyword.SHAPE)
	public void setGeometry(final IShape newGeometry);

	public abstract boolean dead();

	/**
	 * Returns the agent which hosts the population of this agent.
	 * 
	 * @return
	 */
	@getter(IKeyword.HOST)
	public abstract IAgent getHost();

	@setter(IKeyword.HOST)
	public abstract void setHost(final IAgent macroAgent);

	@Override
	public abstract void dispose();

	public abstract void schedule(IScope scope) throws GamaRuntimeException;

	public abstract Object getAttribute(final Object index);

	public abstract void setAttribute(final String name, final Object val);

	/**
	 * Allows to set attributes that will be accessed by the "read" or "get" operators. Used for
	 * GIS/CSV attributes
	 * @param map
	 */
	public abstract void setExtraAttributes(final Map<Object, Object> map);

	public abstract int getIndex();

	public abstract void setIndex(int index);

	public String getSpeciesName();

	public abstract ISpecies getSpecies();

	public IPopulation getPopulation();

	public abstract boolean isInstanceOf(final ISpecies s, boolean direct);

	public void setHeading(Integer heading);

	public Integer getHeading();

	public abstract void die() throws GamaRuntimeException;

	public abstract void updateAttributes(IScope scope) throws GamaRuntimeException;

	public abstract Object getDirectVarValue(IScope scope, String s) throws GamaRuntimeException;

	public void setDirectVarValue(IScope scope, String s, Object v) throws GamaRuntimeException;

	public abstract boolean contains(IAgent component);

	/**
	 * @throws GamaRuntimeException
	 *             Finds the corresponding population of a species from the "viewpoint" of this
	 *             agent.
	 * 
	 *             An agent can "see" the following populations:
	 *             1. populations of its species' direct micro-species;
	 *             2. population of its species; populations of its peer species;
	 *             3. populations of its direct&in-direct macro-species and of their peers.
	 * 
	 * @param microSpecies
	 * @return
	 */
	public abstract IPopulation getPopulationFor(final ISpecies microSpecies);

	/**
	 * @throws GamaRuntimeException
	 *             Finds the corresponding population of a species from the "viewpoint" of this
	 *             agent.
	 * 
	 *             An agent can "see" the following populations:
	 *             1. populations of its species' direct micro-species;
	 *             2. population of its species; populations of its peer species;
	 *             3. populations of its direct&in-direct macro-species and of their peers.
	 * 
	 * @param speciesName the name of the species
	 * @return
	 */
	public abstract IPopulation getPopulationFor(final String speciesName);

	/**
	 * Initialize Populations to manage micro-agents.
	 */
	public abstract void initializeMicroPopulations(IScope scope);

	public abstract void initializeMicroPopulation(IScope scope, String name);

	/**
	 * Returns a list of populations of (direct) micro-species.
	 * 
	 * @return
	 */
	public abstract IList<IPopulation> getMicroPopulations();

	/**
	 * Returns the population of the specified (direct) micro-species.
	 * 
	 * @param microSpeciesName
	 * @return
	 */
	public abstract IPopulation getMicroPopulation(String microSpeciesName);

	/**
	 * Returns the population of the specified (direct) micro-species.
	 * 
	 * @param microSpecies
	 * @return
	 */
	public abstract IPopulation getMicroPopulation(ISpecies microSpecies);

	/**
	 * Verifies if this agent contains micro-agents or not.
	 * 
	 * @return true if this agent contains micro-agent(s)
	 *         false otherwise
	 */
	public abstract boolean hasMembers();

	public abstract List<IAgent> getMacroAgents();

	/**
	 * Acquires the object's intrinsic lock.
	 * 
	 * Solves the synchronization problem between Execution Thread and Event Dispatch Thread.
	 * 
	 * The synchronization problem may happen when
	 * 1. The Event Dispatch Thread is drawing an agent while the Execution Thread tries to it;
	 * 2. The Execution Thread is disposing the agent while the Event Dispatch Thread tries to draw
	 * it.
	 * 
	 * To avoid this, the corresponding thread has to invoke "acquireLock" to lock the agent before
	 * drawing or disposing the agent.
	 * After finish the task, the thread invokes "releaseLock" to release the agent's lock.
	 * 
	 * return
	 * true if the agent instance is available for use
	 * false otherwise
	 */
	public abstract boolean acquireLock();

	/**
	 * Releases the object's intrinsic lock.
	 */
	public abstract void releaseLock();

	/**
	 * Verifies if this agent can capture other agent as the specified micro-species.
	 * 
	 * An agent A can capture another agent B as newSpecies if the following conditions are correct:
	 * 1. other is not this agent;
	 * 2. other is not "world" agent;
	 * 3. newSpecies is a (direct) micro-species of A's species;
	 * 4. newSpecies is a direct sub-species of B's species.
	 * 
	 * @param other
	 * @return
	 *         true if this agent can capture other agent
	 *         false otherwise
	 */
	public abstract boolean canCapture(IAgent other, ISpecies newSpecies);

	/**
	 * Captures some agents as micro-agents with the specified micro-species as their new species.
	 * 
	 * @param microSpecies the species that the captured agents will become, this must be a
	 *            micro-species of this agent's species.
	 * @param microAgents
	 * @return
	 * @throws GamaRuntimeException
	 */
	public abstract IList<IAgent> captureMicroAgents(IScope scope, final ISpecies microSpecies,
		final IList<IAgent> microAgents) throws GamaRuntimeException;

	public abstract IAgent captureMicroAgent(IScope scope, final ISpecies microSpecies, final IAgent microAgent)
		throws GamaRuntimeException;

	/**
	 * Releases some micro-agents of this agent.
	 * 
	 * @param microAgents
	 * @return
	 * @throws GamaRuntimeException
	 */
	public abstract IList<IAgent> releaseMicroAgents(IScope scope, final IList<IAgent> microAgents)
		throws GamaRuntimeException;

	/**
	 * Migrates some micro-agents from one micro-species to another micro-species of this agent's
	 * species.
	 * 
	 * @param microAgent
	 * @param newMicroSpecies
	 * @return
	 */
	public abstract IList<IAgent> migrateMicroAgents(IScope scope, final IList<IAgent> microAgents,
		final ISpecies newMicroSpecies);

	/**
	 * Migrates some micro-agents from one micro-species to another micro-species of this agent's
	 * species.
	 * 
	 * @param microAgent
	 * @param newMicroSpecies
	 * @return
	 */
	public abstract IList<IAgent> migrateMicroAgents(IScope scope, final ISpecies oldMicroSpecies,
		final ISpecies newMicroSpecies);

	/**
	 * Tells this agent that the host has changed its shape.
	 * This agent will then ask the topology to add its shape to the new ISpatialIndex.
	 */
	public abstract void hostChangesShape();

	public abstract void computeAgentsToSchedule(final IScope scope, final IList<IAgent> list);

	public ISimulationAgent getSimulation();

	public IScheduler getScheduler();

	public IModel getModel();

	public IExperimentAgent getExperiment();

	public IScope getScope();

	public abstract boolean isInstanceOf(String skill, boolean direct);

}