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
	Map<HierarchyDependency, Map<Statement, Abstract1>> interfaceMap = new HashMap<HierarchyDependency, Map<Statement, Abstract1>>(); //Interface containing current abstractEnvMap (cf. SequAbstractInterpreter) for every subgraf
	Map<HierarchyDependency, Map<Statement, Abstract1>> abstResultsHypergrafMap = new HashMap<HierarchyDependency, Map<Statement, Abstract1>>(); 
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
			copyAbstInterface = new HashMap<HierarchyDependency, Map<Statement, Abstract1>>(interfaceMap);
			for (HierarchyDependency dependency : hierarchyOrder.getDependencies()) {
				Abstract1 i = joinInterface(dependency);
				SequAbstractInterpreter seqAI = new SequAbstractInterpreter(dependency, man, env, i); //TODO Interface übergeben
				seqAI.runAnalysis();
				abstResultsHypergrafMap.put(dependency, seqAI.getDeepcopyAbstractEnvMap());
				//TODO   Map<Statement, Abstract1> abstEnvMapSubgraf = seqAI.getDeepcopyAbstractEnvMap();
				fullLog += seqAI.getOutputString(); //TODO hier werden die Iterationen nicht deutlich (bezüglich Interface)
				//initialize Interface
				if (interfaceMap.get(dependency) == null) {
					interfaceMap.put(dependency, seqAI.getInterfaceOut());
					//abstInterface.put(dependency, abstEnvMapSubgraf); //FIXME führt zu überapproximation. in SeqAbstractInterpreter sollte ein spezielles Interface nur bei Aktionen erstellt werden
				} else {
					interfaceMap.put(dependency, Util.joinInterface(man, interfaceMap.get(dependency), seqAI.getInterfaceOut()));
				}
				
			}	
			
		} while (!Util.equalsInterface(man, interfaceMap, copyAbstInterface)); //FIXME FUNKTIONIERT DAS?
	}
	
	public String getOut() {
		return fullLog;
	}
	
	private Abstract1 joinInterface(HierarchyDependency currentDependency) throws ApronException {
		Abstract1 absOut = new Abstract1(man, env, true);
		for (HierarchyDependency d : hierarchyOrder.getDependencies()) {
			if (d != currentDependency) {
				if (interfaceMap.get(d) != null) {
					for(Abstract1 a : interfaceMap.get(d).values()){
						absOut.join(man, a);
					}
				}
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
				for (Statement statement : interfaceMap.get(subgrafTemp).keySet()) {
					abstInterfaceSubgraf.join(man, interfaceMap.get(subgrafTemp).get(statement));
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
