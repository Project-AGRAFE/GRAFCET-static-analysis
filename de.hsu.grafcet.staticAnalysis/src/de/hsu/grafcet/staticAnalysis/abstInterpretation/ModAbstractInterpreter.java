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
	Map<HierarchyDependency, Map<Statement, Abstract1>> interfaceMap = new HashMap<HierarchyDependency, Map<Statement, Abstract1>>(); //Interface containing current abstractEnvMap (cf. SequAbstractInterpreter) for every subgraf
	Map<HierarchyDependency, Map<Statement, Abstract1>> abstResultsHypergrafMap = new HashMap<HierarchyDependency, Map<Statement, Abstract1>>(); 
	Manager man = new Box();
	Environment env;
//	String[] varNames;
//	Set<VariableDeclaration> variableDeclarationSet = new LinkedHashSet<VariableDeclaration>();
	String fullLog = "";
	
	public Environment getEnv() {
		return env;
	}
	public Abstract1 getAbstract1FromStatement(HierarchyDependency d, Statement s) {
		return abstResultsHypergrafMap.get(d).get(s);
	}
	
	
	public ModAbstractInterpreter(HierarchyOrder hierarchyOrder) {
		super();
		this.hierarchyOrder = hierarchyOrder;
		this.hypergraf = hierarchyOrder.getHypergraf();
		env = generateEnvironment(hypergraf, false);
	}


	public void runAnalysis() throws ApronException {
		modularAbstractInterpretation();
	}
	
	/**
	 * Analysis is performed for every dependency (combination of subgraf and a initial situation).
	 * @throws ApronException
	 */
	private void modularAbstractInterpretation() throws ApronException {
		Map<HierarchyDependency, Map<Statement, Abstract1>>  copyInterfaceMap;
		int iteration = 1;
		do {
			fullLog += " \n\n ========  iteration " + iteration + " ======== \n\n";
			iteration ++;
			//shallow copy (!) the interface
			copyInterfaceMap = new HashMap<HierarchyDependency, Map<Statement, Abstract1>>(interfaceMap);
			for (HierarchyDependency dependency : hierarchyOrder.getDependencies()) {
				
				Abstract1 i = joinInterface(dependency, copyInterfaceMap);
				SequAbstractInterpreter seqAI = new SequAbstractInterpreter(dependency, man, env, i);
				seqAI.runAnalysis();
				abstResultsHypergrafMap.put(dependency, seqAI.getDeepcopyAbstractEnvMap());
				fullLog += seqAI.getOutputString(); 
				//initialize Interface
				if (interfaceMap.get(dependency) == null) {
					interfaceMap.put(dependency, seqAI.getInterfaceOut());
				} else {
					interfaceMap.put(dependency, Util.joinInterface(man, interfaceMap.get(dependency), seqAI.getInterfaceOut()));
				}
			}	
		} while (!Util.equalsInterface(man, interfaceMap, copyInterfaceMap));
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
	
	/**
	 * Bulids a String[] varnames from the output and internal variables of the Grafcet in hypergraf
	 */
	public static Environment generateEnvironment(Hypergraf hypergraf, boolean includingInputs) {
		Set<VariableDeclaration> variableDeclarationSet = new LinkedHashSet<VariableDeclaration>();
		for (VariableDeclaration var : hypergraf.getGlobalGrafcet().getVariableDeclarationContainer().getVariableDeclarations()) {
			//collect internal and output variables
			if (var.getVariableDeclarationType().equals(VariableDeclarationType.OUTPUT) 
					|| var.getVariableDeclarationType().equals(VariableDeclarationType.INTERNAL)
					|| (var.getVariableDeclarationType().equals(VariableDeclarationType.INPUT) && includingInputs)) {
				
				variableDeclarationSet.add(var);
			}
		}
		String[] varNames = new String[variableDeclarationSet.size()];
		int i = 0;
		for (VariableDeclaration var : variableDeclarationSet) {
			varNames[i] = var.getName();
			i++;
		}
		return new Environment(varNames, new String[] {});
	}
	
	
	
	@Override
	public String toString() {
		String out = "\n\n\n#################  Final interfaces  ###############";
		for(HierarchyDependency d : interfaceMap.keySet()) {
			out += Util.printAbstMap(d.getInferiorName(), interfaceMap.get(d), env, man);
		}
		out += "\n\n\n#################  Final abstract values  ###############";
		for(HierarchyDependency d : abstResultsHypergrafMap.keySet()) {
			out += Util.printAbstMap(d.getInferiorName(), abstResultsHypergrafMap.get(d), env, man);
		}
		return out;
	}
	

	
}
