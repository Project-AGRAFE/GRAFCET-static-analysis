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

public class ThreadModAbstractInterpreter {

	HierarchyOrder hierarchyOrder;
	Hypergraf hypergraf;
	Map<HierarchyDependency, Map<Statement, Abstract1>> abstInterface = new HashMap<HierarchyDependency, Map<Statement, Abstract1>>(); //Interface containing current abstractEnvMap (cf. SequAbstractInterpreter) for every subgraf
//	Map<Statement, Abstract1> abstEnvMapHypergraf = new HashMap<Statement, Abstract1>(); //named TE by Kusano et. al
	Manager man = new Box();
	Environment env;
	String[] varNames;
	Set<VariableDeclaration> variableDeclarationSet = new LinkedHashSet<VariableDeclaration>();
	
	
	
	
	public ThreadModAbstractInterpreter(HierarchyOrder hierarchyOrder) {
		super();
		this.hierarchyOrder = hierarchyOrder;
		this.hypergraf = hierarchyOrder.getHypergraf();
		collectVariableNames();
	}


	public void runAnalysis() throws ApronException {
		threadModularAbstractInterpretation();
	}
	
	/**
	 * Analysis is performed for every dependency (combination of subgraf and a initial situation).
	 * @throws ApronException
	 */
	private void threadModularAbstractInterpretation() throws ApronException {
		Map<HierarchyDependency, Map<Statement, Abstract1>>  copyAbstInterface;
		
		do {
			//shallow copy (!) the interface
			copyAbstInterface = new HashMap<HierarchyDependency, Map<Statement, Abstract1>>(abstInterface);
			for (HierarchyDependency dependency : hierarchyOrder.getDependencies()) {
				SequAbstractInterpreter seqAI = new SequAbstractInterpreter(dependency, man, env);
				Map<Statement, Abstract1> abstEnvMapSubgraf = seqAI.getDeepcopyAbstractEnvMap();
				abstInterface.put(dependency, Util.joinCopyEnvMaps(man, abstInterface.get(dependency), abstEnvMapSubgraf));
			}	
			
		} while (!Util.equalsInterface(man, abstInterface, copyAbstInterface)); //FIXME FUNKTIONIERT DAS?
	}
	
	
	
	/**
	 * 	line 7 in Alg. 2 in paper of Kusano et. al
	 * @param subgraf
	 * @return
	 * @throws ApronException
	 */
	private Abstract1 buildInterfaceSubgraf(Subgraf subgraf) throws ApronException {
		Abstract1 abstInterfaceSubgraf = new Abstract1(man, env, true);
		for (Subgraf subgrafTemp : hypergraf.getSubgrafs()){
			if (subgraf != subgrafTemp) {
				for (Statement statement : abstInterface.get(subgrafTemp).keySet()) {
					abstInterfaceSubgraf.join(man, abstInterface.get(subgrafTemp).get(statement));
				}
			}
		}
		return abstInterfaceSubgraf;
	}


	/**
	 * Bulids a String[] varnames from the output and internal variables of the Grafcet in hypergraf
	 */
	private void collectVariableNames() {
		for (VariableDeclaration var : hypergraf.getGlobalGrafcet().getVariableDeclarationContainer().getVariableDeclarations()) {
			//collect internal and output variables
			if (var.getVariableDeclarationType().equals(VariableDeclarationType.OUTPUT) || var.getVariableDeclarationType().equals(VariableDeclarationType.INTERNAL)) {
				variableDeclarationSet.add(var);
			}
		}
		varNames = new String[variableDeclarationSet.size()];
		int i = 0;
		for (VariableDeclaration var : variableDeclarationSet) {
			varNames[i] = var.getName();
			i++;
		}
		
		collectVariableNames();
		env = new Environment(varNames, new String[] {});
	}
	
	
	
	
	

	
}
