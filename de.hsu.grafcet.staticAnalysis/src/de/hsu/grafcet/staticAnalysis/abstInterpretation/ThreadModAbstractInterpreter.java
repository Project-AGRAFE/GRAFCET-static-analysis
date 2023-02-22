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
	String fullLog = "";
	
	
	
	
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
				Abstract1 i = joinInterface(dependency);
				SequAbstractInterpreter seqAI = new SequAbstractInterpreter(dependency, man, env, i); //TODO Interface übergeben
				seqAI.runAnalysis();
				abstInterface.put(dependency, seqAI.getInterfaceOut());
				Map<Statement, Abstract1> abstEnvMapSubgraf = seqAI.getDeepcopyAbstractEnvMap();
				fullLog += seqAI.getOutputString();
				//initialize Interface
				if (abstInterface.get(dependency) == null) {
					abstInterface.put(dependency, abstEnvMapSubgraf); //FIXME führt zu überapproximation. in SeqAbstractInterpreter sollte ein spezielles Interface nur bei Aktionen erstellt werden
				}
				abstInterface.put(dependency, Util.joinCopyEnvMaps(man, abstInterface.get(dependency), abstEnvMapSubgraf));
			}	
			
		} while (!Util.equalsInterface(man, abstInterface, copyAbstInterface)); //FIXME FUNKTIONIERT DAS?
	}
	
	public String getOut() {
		return fullLog;
	}
	
	private Abstract1 joinInterface(HierarchyDependency currentDependency) throws ApronException {
		Abstract1 absOut = new Abstract1(man, env, true);
		for (HierarchyDependency d : hierarchyOrder.getDependencies()) {
			if (d != currentDependency) {
				for(Abstract1 a : abstInterface.get(d).values()){
					absOut.join(man, a);
				}
			} else {
				
			}
		}
		return absOut;
	}
	
	
	//TODO obige Änderungen Testen: einen Abhängigen Teilgrafen erstellen und einen unabhägngigen. Bzw. mit vars, die Abhängig sind und vars, die nicht abhängig sind, um zu sehen, die Algorithmus darauf reagiert
	
	
	
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
		env = new Environment(varNames, new String[] {});
	}
	
	
	
	@Override
	public String toString() {
		String out = "";
		for(HierarchyDependency d : abstInterface.keySet()) {
			out += Util.printAbstMap(d.getInferiorName(), abstInterface.get(d), env, man);
		}
		return out;
	}
	

	
}
