package de.hsu.grafcet.staticAnalysis.abstInterpretation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apron.Abstract1;
import apron.ApronException;
import apron.Box;
import apron.Environment;
import apron.Interval;
import apron.Manager;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyDependency;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyOrder;
import de.hsu.grafcet.staticAnalysis.hypergraf.Edge;
import de.hsu.grafcet.staticAnalysis.hypergraf.Hypergraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Statement;
import de.hsu.grafcet.staticAnalysis.hypergraf.Subgraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Vertex;
import de.hsu.grafcet.staticAnalysis.structuralAnalysis.CustomArrayDeque;
import terms.VariableDeclaration;

public class ConcurrAbstractInterpreter {
	
	Map<Statement, Abstract1> interferenceStatementAbsEnvMap = new HashMap<Statement, Abstract1>();
	Map<Statement, Abstract1> abstractEnvMap = new HashMap<Statement, Abstract1>(); //abstractEnv for every statement
	
	Manager man = new Box();
	Environment env;
	
	
	CustomArrayDeque<Statement> worklist = new CustomArrayDeque<Statement>();
	HierarchyDependency dependency;
	Subgraf subgraf;
	Set<Statement> statements;
	

	public ConcurrAbstractInterpreter() throws ApronException {
		// TODO Auto-generated constructor stub
		
		
		
		statements = subgraf.getStatements();
		subgraf.resetStatementVisitCounter();
		abstractEnvMap = initializeAbstractEnvMap(subgraf);
	}
	
	private void concurrentAbstracInterpretation() throws ApronException {
		//initialize worklist
		worklist.addAll(dependency.getInitiallyActiveVertices());
		
		while (!worklist.isEmpty()) {
			Statement n = worklist.pollFirst();
			Abstract1 interferenceN = new Abstract1(man, env, true);
			if (n instanceof Vertex) {
				interferenceN = calculateInterferenceN(interferenceStatementAbsEnvMap);
			}
			TransferFunction transfer = new TransferFunction(n, abstractEnvMap.get(n), man, env, interferenceN);
			Abstract1 e = transfer.transferInterface().joinCopy(man, interferenceN);
			for (Statement nDown : subgraf.getDownstream(n)) {
				if (!e.isIncluded(man, abstractEnvMap.get(nDown))) {
					if(nDown instanceof Edge) {
						setInterfaceEntry(transfer.getInterfaceEntry(), n);
						if (transfer.getInterfaceEntry() != null) {
							if (!transfer.getInterfaceEntry().isBottom(man)) {
								worklist.addAll(dependency.getConcurrentVertices((Vertex) n));
							}							
						}
						
						//TODO hier weiter machen mit Zeile 15 im Algorithmus
					}
					
					
				}
			}
		}
	}
	
	private Abstract1 calculateInterferenceN(Map<Statement, Abstract1> interferenceStatementAbsEnvMap) {
		
	}
	
	private Map<Statement, Abstract1>  initializeAbstractEnvMap(Subgraf subgraf) throws ApronException{
		Map<Statement, Abstract1> abstractEnvMap = new HashMap<Statement, Abstract1>();
		//set abstractEnvMap to bottom
		for (Statement n : subgraf.getStatements()) {
			Abstract1 abstractEnvN = new Abstract1(man, env, true);
			abstractEnvMap.put(n, abstractEnvN);
		}
		//set abstractEnv(n) for entry statement to [0, 0]
		Interval[] initialIntervals = new Interval[env.getVars().length];
		for (Statement n : dependency.getInitiallyActiveVertices()) {
			for (int i = 0; i < initialIntervals.length; i++) {
				initialIntervals[i] = new Interval(0, 0);
			}
			Abstract1 abstractEnvN = new Abstract1(man, env, env.getVars(), initialIntervals);
			abstractEnvMap.get(n).join(man, abstractEnvN);
		}
		return abstractEnvMap;
	}
	
	private void setInterfaceEntry(Abstract1 interfaceEntryN, Statement n) throws ApronException {
		if (interfaceEntryN != null) {
			if (interferenceStatementAbsEnvMap.containsKey(n)) {
				interferenceStatementAbsEnvMap.put(n, interferenceStatementAbsEnvMap.get(n).joinCopy(man, interfaceEntryN));
			} else {
				interferenceStatementAbsEnvMap.put(n, interfaceEntryN);
			}
		}
	}
	
	public void runAnalysis() {
		concurrentAbstracInterpretation();
	}
	

	
	

}
