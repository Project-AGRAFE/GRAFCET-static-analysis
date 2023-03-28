package de.hsu.grafcet.staticAnalysis.hierarchyOrder;

import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apron.Abstract1;
import apron.ApronException;
import apron.Box;
import apron.Environment;
import de.hsu.grafcet.Grafcet;
import de.hsu.grafcet.InitializableType;
import de.hsu.grafcet.Step;
import de.hsu.grafcet.staticAnalysis.abstInterpretation.RaceConditionDetecter;
import de.hsu.grafcet.staticAnalysis.abstInterpretation.ThreadModAbstractInterpreter;
import de.hsu.grafcet.staticAnalysis.hypergraf.Edge;
import de.hsu.grafcet.staticAnalysis.hypergraf.Hypergraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Statement;
import de.hsu.grafcet.staticAnalysis.hypergraf.Subgraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Vertex;
import de.hsu.grafcet.staticAnalysis.hypergrafTransromation.HypergrafcetGenerator;
import de.hsu.grafcet.staticAnalysis.structuralAnalysis.StructuralConcurrencyAnalyzer;
import terms.VariableDeclaration;

public class HierarchyOrder {

	private Hypergraf hypergraf;
//	LinkedHashSet<HierarchyDependency> sequentialDependencies = new LinkedHashSet<HierarchyDependency>();
	private LinkedHashSet<ParallelDependency> parallelDependencies;
	private LinkedHashSet<HierarchyDependency> dependencies = new LinkedHashSet<HierarchyDependency>();
//	Map<Vertex, Set<Vertex>> concurrentVertices; //zu rechenintensiv? tbd ob das Sinn macht?
	private ThreadModAbstractInterpreter tmai;

	
	public HierarchyOrder(Grafcet grafcet) {
		super();
		HypergrafcetGenerator hg = new HypergrafcetGenerator(grafcet, this);
		hg.generate();		
	}	
	
	
	
	/**
	 * 
	 * check HierarchOrder according to Lesage et al.:
	 * @return
	 */
	public String analyzeHierarchyOrderForPartialOrder() {
		//check for cycles
		//calculate depth
		return new String("Result of hierarchy check for partial order");
	}
	
	public String analyzeReachabilityAndConcurrency() {
		String out = "";
		for (HierarchyDependency dependency : dependencies) {
			out += "Partial Grafcet " + dependency.getInferior() + "\n Dependency: ";
			out += StructuralConcurrencyAnalyzer.reachabilityConcurrencyAnalysis(dependency, false, true);
		}
		return out;
	}
	
	public void runAbstractInterpretation() throws ApronException {
		tmai = new ThreadModAbstractInterpreter(this);
		tmai.runAnalysis();
	}
	
	public String getAIResult() {
		return tmai.toString();
	}
	public String getAIFullLog() {
		return tmai.getFullLog();
	}
	
	public String analyzeRecaConditions() throws ApronException {
		RaceConditionDetecter rcd = new RaceConditionDetecter(this);
		return rcd.runAnalysis();
	}
	public void analyzeDeadlocks() {
		
	}
	
	
	
	
	public LinkedHashSet<HierarchyDependency> getDependencies() {
		return dependencies;
	}
	public void setDependencies(LinkedHashSet<HierarchyDependency> dependencies) {
		this.dependencies = dependencies;
	}
	
	public Hypergraf getHypergraf() {
		return hypergraf;
	}
	public void setHypergraf(Hypergraf hypergraf) {
		this.hypergraf = hypergraf;
	}

	public Environment getApronEnvironment() {
		if (tmai == null) {
			return null;
		}
		return this.tmai.getEnv();
	}
	/**
	 * joins abstract environment for s for all including dependencies
	 * @param s
	 * @return
	 * @throws ApronException 
	 */
	public Abstract1 getAbstract1FromStatement (Statement s) throws ApronException {
		if (tmai == null) {
			return null;
		}
		Abstract1 abs = new Abstract1(new Box(), getApronEnvironment(), true);
		for (HierarchyDependency d : getContainingDependencies(s)) {
			abs.join(new Box(), tmai.getAbstract1FromStatement(d, s));
		}
		
		return abs;
	}
	
	
	/**
	 * returns set of dependencies in which s is part of inferior subgraf
	 * @param s
	 * @return
	 */
	private Set<HierarchyDependency> getContainingDependencies(Statement s){
		Set<HierarchyDependency> containingDependencies = new LinkedHashSet<HierarchyDependency>();
		
		if (s instanceof Vertex) {
			for (HierarchyDependency d:  dependencies) {
				if (d.getInferior().getVertices().contains(s)) {
					containingDependencies.add(d);
				}
			}
		} else if (s instanceof Edge) {
			for (HierarchyDependency d:  dependencies) {
				if (d.getInferior().getEdges().contains(s)) {
					containingDependencies.add(d);
				}
			}
		}
		

		return containingDependencies;
	}
	
	public String getAllConcurrentStepsString() {
		String out = "";
		Map<Vertex, Set<Vertex>> concrrentStepMap = getAllConcurrentSteps();
		for (Vertex v : concrrentStepMap.keySet()) {
			out += v + ":  " + concrrentStepMap.get(v) + "\n";
		}
		return out;
	}
	
	public Map<Vertex, Set<Vertex>> getAllConcurrentSteps(){
		Map<Vertex, Set<Vertex>> concrrentStepMap = new HashMap<Vertex, Set<Vertex>>();
		for (Subgraf s : this.hypergraf.getSubgrafs()) {
			for (Vertex v : s.getVertices()) {
				concrrentStepMap.put(v, null);
			}
		}
		for (Vertex v : concrrentStepMap.keySet()) {
			concrrentStepMap.put(v, getConcurrentSteps(v));
		}
		return concrrentStepMap;
		
	}
	
	/**
	 * returns all concurrent vertices to @param vertex. 
	 * Concurrent vertices are: 
	 * - concurrent vertices from the same subgraf
	 * - reachable vertices from parallel hierarchical dependencies (parallelDependencies are used for calculation, as well as a depth search of inferior dependencies)
	 * Concurrent vertices are NOT:
	 * - superior trigger vertices
	 * - reachable steps from inferior dependencies induces by @param vertex
	 * @param vertex
	 * @return
	 */
	
	public Set<Vertex> getConcurrentSteps(Vertex vertex){
		if (parallelDependencies == null) {
			setParallelDependencies();
		}
		Set<Vertex> concurrentSteps = new LinkedHashSet<Vertex>();
		Set<HierarchyDependency> containingDependencies = new LinkedHashSet<HierarchyDependency>();
		Subgraf containingSubgraf = null;
		
		for (HierarchyDependency d: dependencies) {
			if (d.getReachableVertices().contains(vertex)) {
				containingDependencies.add(d);
				if (d.getConcurrentVertices().get(vertex) != null) {
					concurrentSteps.addAll(d.getConcurrentVertices().get(vertex));
				}
				//concurrentSteps.add(d.getSuperiorTriggerVertex());
				containingSubgraf = d.getInferior(); //sollte immer der selbe sein
			}
		}
		if (containingDependencies.isEmpty() || containingSubgraf == null) {
			throw new IllegalArgumentException("Vertex not included in Hierarchy-Order");
		}
		//get concurrent dependencies
		Set<HierarchyDependency> concurrentDependencies = new LinkedHashSet<HierarchyDependency>();
		for (HierarchyDependency d : containingDependencies) {
			concurrentDependencies.addAll(getConcurrentDependencies(d));
		}
		//Remove all dependencies that have containingSubgraf as inferiorSubgraf 
		//This is the case of a "diamond" in the hierarchy order (which is a potential conflict)
		concurrentDependencies.removeAll(containingDependencies);
		
		for (HierarchyDependency concurrentDependency : concurrentDependencies) {
			concurrentSteps.addAll(concurrentDependency.getReachableVertices());
		}
		return concurrentSteps;
	}
	/**
	 * Runs a depth first seach on the hierarchy order starting with d and returns all visited hierarchy dependencies
	 * @param d
	 * @return
	 */
	

	
	private Set<HierarchyDependency> getConcurrentDependencies(HierarchyDependency dependency){
		Set<HierarchyDependency> concurrentDirectDependencies= new LinkedHashSet<HierarchyDependency>();
		
		//search for parallel dependencies
		for (ParallelDependency parallelDep : parallelDependencies) {
			if (parallelDep.getDependencies().contains(dependency)) {
				concurrentDirectDependencies.addAll(parallelDep.getDependencies());
				concurrentDirectDependencies.remove(dependency);
			}
		}
		
		//depth search
		Set<HierarchyDependency> concurrentInferiorDep= new LinkedHashSet<HierarchyDependency>();
		for (HierarchyDependency concurrentDep : concurrentDirectDependencies) {
			concurrentInferiorDep.addAll(getAllInferiorDependencies(concurrentDep));
		}
		
		//get superior dependencies --> recursion
		Set<HierarchyDependency> concurrentSuperiorDependencies = new LinkedHashSet<HierarchyDependency>();
		for (HierarchyDependency superiorDep : getSuperiorDependencies(dependency.getSuperior())) {
			concurrentSuperiorDependencies.addAll(getConcurrentDependencies(superiorDep));
		}
		
		//TODO: Es reicht ein Set aus, wenn alles funktioniert
		concurrentDirectDependencies.addAll(concurrentInferiorDep);
		concurrentDirectDependencies.addAll(concurrentSuperiorDependencies);
		return concurrentDirectDependencies;
	}
	
	private Set<HierarchyDependency> getAllInferiorDependencies(HierarchyDependency d){
		Set<HierarchyDependency> inferiorDependencies = new HashSet<HierarchyDependency>();
		Subgraf inferior = d.getInferior();
		List<HierarchyDependency> stack = new ArrayList<HierarchyDependency>();
		stack = getDirectInferiorDependencies(inferior);
		while (!stack.isEmpty()) {
			HierarchyDependency dep = stack.get(0);
			stack.remove(0);
			
			inferiorDependencies.addAll(getAllInferiorDependencies(dep));
		}
		return inferiorDependencies;
	}
	
	private List<HierarchyDependency> getDirectInferiorDependencies(Subgraf superior) {
		List<HierarchyDependency> inferiorDependencies = new ArrayList<HierarchyDependency>();
		for (HierarchyDependency d : dependencies) {
			if (d.getSuperior().equals(superior)) {
				inferiorDependencies.add(d);
			}
		}
		return inferiorDependencies;
	}
	
	private Set<HierarchyDependency> getSuperiorDependencies(ISubgraf inferior){
		Set<HierarchyDependency> superiorDependencies = new LinkedHashSet<HierarchyDependency>();
		if(inferior instanceof Hypergraf) {
			return superiorDependencies; //root of the hierarchy order
		} else {
			for (HierarchyDependency d : dependencies) {
				if (d.getInferior().equals(inferior)) {
					superiorDependencies.add(d);
				}
			}
			return superiorDependencies;
		}
	}
	
	private void setParallelDependencies() {
		parallelDependencies = new LinkedHashSet<ParallelDependency>();
		List<HierarchyDependency> dependenciesCopy = new ArrayList<HierarchyDependency>(dependencies);
		while (!dependenciesCopy.isEmpty()) {
			ParallelDependency pd = new ParallelDependency();
			HierarchyDependency d = dependenciesCopy.get(0);
			dependenciesCopy.remove(0);
			for (HierarchyDependency d2 : dependenciesCopy) {
				if (isParallel(d, d2)) {
					pd.addDependency(d);
					pd.addDependency(d2);
				}
			}
			if (!pd.getDependencies().isEmpty()) {
				parallelDependencies.add(pd);
				dependenciesCopy.removeAll(pd.getDependencies()); //um zu verhindern, dass parallel dependencies mehrfach hinzugefügt wird
			}
		}	
		
	}
	
	private boolean isParallel(HierarchyDependency d1, HierarchyDependency d2) {
		for (HierarchyDependency d : dependencies) {
			if (d.getReachableVertices().contains(d1.getSuperiorTriggerVertex()) 
					&& d.getReachableVertices().contains(d2.getSuperiorTriggerVertex())) {
				if (d.getConcurrentVertices().get(d1.getSuperiorTriggerVertex())
						.contains(d2.getSuperiorTriggerVertex())) {
					return true;
				}
			}
		}
		return false;
	}
	
	
}
