package de.hsu.grafcet.staticAnalysis.abstInterpretation;

import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import apron.*;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyDependency;
import de.hsu.grafcet.staticAnalysis.hypergraf.*;
import de.hsu.grafcet.staticAnalysis.structuralAnalysis.CustomArrayDeque;
import terms.*;
import de.hsu.grafcet.*;

public class SequAbstractInterpreter {
	
	HierarchyDependency dependency;
	Subgraf subgraf;
	Manager man = new Box();
	Set<VariableDeclaration> variableDeclarationSet = new LinkedHashSet<VariableDeclaration>();
//	String[] varNames; //in Thread-Mod Verschoben
	Environment env;
	Map<Statement, Abstract1> abstractEnvMap = new HashMap<Statement, Abstract1>(); //abstractEnv for every statement
	String outputString;
	Abstract1 interfaceIn; 	//joined interface for multi-threading approach
	Map<Statement, Abstract1> interfaceOut = new HashMap<Statement, Abstract1>(); //abstractEnv for every statement
	
	public SequAbstractInterpreter(HierarchyDependency dependency, Manager man, Environment env) {
	this.dependency = dependency;
	this.subgraf = dependency.getInferior();
	this.man = man;
	this.env = env;
		if(!subgraf.isSequential()) {
			//throw new IllegalArgumentException("provided grafcet is not sequential");
		}
	}
	
	public SequAbstractInterpreter(HierarchyDependency dependency, Manager man, Environment env, Abstract1 interfaceIn) {
		this(dependency, man, env);
		this.interfaceIn = interfaceIn;
	}
	
	public void runAnalysis() throws ApronException {
		sequentialAbstractInterpretation();
	}
	
	
//	TODO Interface muss berücksichtigt werden!
	
	
	private void sequentialAbstractInterpretation() throws ApronException{
		//For debugging:
//		if (subgraf.getPartialGrafcet().getName().equals("G0")){
//			System.out.println(subgraf.getPartialGrafcet().getName());
//		}
		
		long start = System.currentTimeMillis();
		
		
		
		String out = "";
			
		out += "\n" + "=== " + subgraf.getPartialGrafcet().getName() + " ===";

		CustomArrayDeque<Statement> worklist = new CustomArrayDeque<Statement>();
		
		Set<Statement> statements = new LinkedHashSet<Statement>();//: Statements S u T
		statements.addAll(subgraf.getVertices());
		statements.addAll(subgraf.getEdges());
		for (Statement s : statements) {
			s.resetVisited();
		}
		
		//set abstractEnvMap to bottom
		for (Statement n : statements) {
			Abstract1 abstractEnvN = new Abstract1(man, env, true);
			abstractEnvMap.put(n, abstractEnvN);
		}
		//set abstractEnv(n) for entry statement to [0, 0]
		//Interval[] initialBox = new Interval[varNames.length];
		Interval[] initialBox = new Interval[env.getVars().length];
		for (Statement n : dependency.getInitiallyActiveVertices()) {
			for (int i = 0; i < initialBox.length; i++) {
				initialBox[i] = new Interval(0, 0);
			}
			Abstract1 absN = new Abstract1(man, env, env.getVars(), initialBox);
			abstractEnvMap.get(n).join(man, absN);
		}
		//initialize worklist
		worklist.addAll(dependency.getInitiallyActiveVertices());

		out += "\n" + Util.printAbstMap("after initialization", abstractEnvMap, env, man);
		//out += "\n" + "==== interface === \n" + Arrays.toString(interfaceIn.toBox(man));
		Map<Statement, Abstract1> interfaceTemp = new HashMap<Statement, Abstract1>();
		interfaceTemp.put(null, interfaceIn);
		out += "\n" + Util.printAbstMap("interference", interfaceTemp, env, man);

		while (!worklist.isEmpty()) {
			Statement statement = worklist.pollFirst();
			
			statement.increaseVisited();
			TransferFunction trans = new TransferFunction(statement, abstractEnvMap.get(statement), man, env, interfaceIn);
//			Abstract1 e = TransferFunction.transfer(statement, abstractEnvMap.get(statement), man, env);
			Abstract1 e = trans.transferInterface();
			//TODO hier irgendow prüfen, ob bei Transition alle pre-steps erreichbar sind
			setInterfaceOut(trans.getInterfaceEntry(), statement);
			for (Statement downstream : getDownstream(statement, subgraf)) {
				if (!e.isIncluded(man, abstractEnvMap.get(downstream))) {
					if(statement.getVisited() > 5) {
//						out += "\n" + "widening ";
						abstractEnvMap.get(downstream).join(man, abstractEnvMap.get(downstream).widening(man, e));
						//reset other widening-counters to make analysis more precise:
						for (Statement s : statements) {
							s.resetVisited();
						}
					} else {
						abstractEnvMap.get(downstream).join(man, e);
					}
					worklist.add(downstream);
//					out += "\n" + Util.printAbstMap("in progress - Step or tran = " + statement.getId(), abstractEnvMap, env, man);
				}
			}
			

		}
		
		
		long end = System.currentTimeMillis();
		long result = end- start;
//		System.out.println( result + " ms for PG " + subgraf.getPartialGrafcet().getName());
		
		out += "\n" + Util.printAbstMap("termination - result", abstractEnvMap, env, man);
		out += "\n" + "\n\n ----";
		outputString = out;
	}
	
	private void setInterfaceOut(Abstract1 interfaceEntry, Statement n) throws ApronException {
		if (interfaceEntry != null) {
			if (interfaceOut.containsKey(n)) {
				interfaceOut.put(n, interfaceOut.get(n).joinCopy(man, interfaceEntry));
			} else {
				interfaceOut.put(n, interfaceEntry);
			}
		}
	}
	public Map<Statement, Abstract1> getInterfaceOut() {
		return interfaceOut;
	}
	
	
	public String getOutputString() {
		return outputString;
	}
		
	private Set<? extends Statement> getDownstream(Statement statement, Subgraf subgraf) {
		if(statement instanceof Vertex) {
			return subgraf.getDownstreamEdges((Vertex) statement);
		} else if (statement instanceof Edge) {
			return ((Edge) statement).getDownstream();
		} 
		return null;		
	}
		
	public Environment getEnv() {
		return env;
	}
	public Map<Statement, Abstract1> getDeepcopyAbstractEnvMap() throws ApronException {
		return Util.deepcopyEnvMap(man, abstractEnvMap);
	}
	public Manager getMan() {
		return man;
	}
	
}
