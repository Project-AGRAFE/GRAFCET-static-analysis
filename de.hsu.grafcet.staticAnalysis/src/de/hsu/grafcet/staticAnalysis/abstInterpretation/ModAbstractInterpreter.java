package de.hsu.grafcet.staticAnalysis.abstInterpretation;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import apron.Abstract1;
import apron.ApronException;
import apron.Box;
import apron.Environment;
import apron.Manager;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyDependency;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyOrder;
import de.hsu.grafcet.staticAnalysis.hypergraf.Hypergraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Statement;
import de.hsu.grafcet.staticAnalysis.hypergraf.Subgraf;
import terms.VariableDeclaration;
import terms.VariableDeclarationType;

public class ModAbstractInterpreter {

	HierarchyOrder hierarchyOrder;
	Hypergraf hypergraf;
	Map<HierarchyDependency, Map<Statement, Abstract1>> interferenceDependencyAbsEnvMap = new HashMap<HierarchyDependency, Map<Statement, Abstract1>>(); //Interference containing current abstractEnvMap (cf. SequAbstractInterpreter) for every subgraf
	Map<HierarchyDependency, Map<Statement, Abstract1>> resultsDependencyAbsEnvMap = new HashMap<HierarchyDependency, Map<Statement, Abstract1>>(); //Results of the abstract interpretation containing the abstract environment for every subgraf
	Manager man = new Box();
	Environment env;
	String fullLog = "";
	
	public Environment getEnv() {
		return env;
	}
	public Abstract1 getAbstract1FromStatement(HierarchyDependency d, Statement s) {
		return resultsDependencyAbsEnvMap.get(d).get(s);
	}
	
	
	public ModAbstractInterpreter(HierarchyOrder hierarchyOrder) {
		super();
		this.hierarchyOrder = hierarchyOrder;
		this.hypergraf = hierarchyOrder.getHypergraf();
		env = Util.generateEnvironment(hypergraf, false);
	}


	public void runAnalysis() throws ApronException {
		modularAbstractInterpretation();
	}
	
	/**
	 * Analysis is performed for every dependency (combination of subgraf and a initial situation).
	 * @throws ApronException
	 */
	private void modularAbstractInterpretation() throws ApronException {
		Map<HierarchyDependency, Map<Statement, Abstract1>>  copyInterferenceMap;
		int iteration = 1;
		do {
			fullLog += " \n\n ========  iteration " + iteration + " ======== \n\n";
			iteration ++;
			//shallow copy (!) the interface
			copyInterferenceMap = new HashMap<HierarchyDependency, Map<Statement, Abstract1>>(interferenceDependencyAbsEnvMap);
			for (HierarchyDependency dependency : hierarchyOrder.getDependencies()) {
				
				Abstract1 i = joinInterface(dependency, copyInterferenceMap);
				SequAbstractInterpreter seqAI = new SequAbstractInterpreter(dependency, man, env, i);
 				seqAI.runAnalysis();
				resultsDependencyAbsEnvMap.put(dependency, seqAI.getDeepcopyAbstractEnvMap());
				fullLog += seqAI.getOutputString(); 
				//initialize Interface
				if (interferenceDependencyAbsEnvMap.get(dependency) == null) {
					interferenceDependencyAbsEnvMap.put(dependency, seqAI.getInterfaceOut());
				} else {
					interferenceDependencyAbsEnvMap.put(dependency, Util.joinInterface(man, interferenceDependencyAbsEnvMap.get(dependency), seqAI.getInterfaceOut()));
				}
			}	
		} while (!Util.equalsInterface(man, interferenceDependencyAbsEnvMap, copyInterferenceMap));
	}

	
	public String getFullLog() {
		return fullLog;
	}
	
	private Abstract1 joinInterface(HierarchyDependency currentDependency, Map<HierarchyDependency, Map<Statement, Abstract1>> interfaceMap) throws ApronException {
		Abstract1 absOut = new Abstract1(man, env, true);
		for (HierarchyDependency d : hierarchyOrder.getDependencies()) {
			if (d != currentDependency) {
				if (interfaceMap.get(d) != null) {
					for (Statement s : interfaceMap.get(d).keySet()) {
						absOut.join(man, interfaceMap.get(d).get(s));
//						System.out.println("dep. " + d.getInferiorName() + ", Statement " + s.getId() + ": " + interfaceMap.get(d).get(s).getBound(man, "Station2_fertig"));
					}
				}
			}
		}
		return absOut;
	}
	

	
	
	
	@Override
	public String toString() {
		String out = "\n\n\n#################  Final interferende  ###############";
		for(HierarchyDependency d : interferenceDependencyAbsEnvMap.keySet()) {
			out += Util.printAbstMap(d.getInferiorName(), interferenceDependencyAbsEnvMap.get(d), env, man);
		}
		out += "\n\n\n#################  Final abstract values  ###############";
		for(HierarchyDependency d : resultsDependencyAbsEnvMap.keySet()) {
			out += Util.printAbstMap(d.getInferiorName(), resultsDependencyAbsEnvMap.get(d), env, man);
		}
		return out;
	}
	

	
}
