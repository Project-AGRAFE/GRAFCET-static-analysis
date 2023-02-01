package de.hsu.grafcet.staticAnalysis.analysis;

import java.lang.management.OperatingSystemMXBean;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import apron.*;
import de.hsu.grafcet.staticAnalysis.hypergraf.*;
import terms.*;
import de.hsu.grafcet.*;

public class AbstractInterpreter {
	
	Hypergraf hypergraf;
	Manager man = new Box();
	Set<VariableDeclaration> variableDeclarationSet = new LinkedHashSet<VariableDeclaration>();
	String[] varNames;
	Environment env;
	Map<Statement, Abstract1> abstractEnvMap = new HashMap<Statement, Abstract1>(); //abstractEnv for every statement
	
	public AbstractInterpreter(Hypergraf hypergrafcet){
		this.hypergraf = hypergrafcet;
		if(!hypergrafcet.isSequential()) {
			//throw new IllegalArgumentException("provided grafcet is not sequential");
		}
	}
	
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
	}
	
	public String sequentialAbstractInterpretation() throws ApronException{
		
		String out = "";
		
		collectVariableNames();
		env = new Environment(varNames, new String[] {});
		
		for (Subgraf subgraf : hypergraf.getSubgrafs()) {
			
//			//TEST
//			if (subgraf.getPartialGrafcet().getName().equals("G2")) {
//			//TSET
			
			out += "\n" + "=== " + subgraf.getPartialGrafcet().getName() + " ===";

			CustomArrayDeque<Statement> worklist = new CustomArrayDeque<Statement>();
			
			Set<Statement> statements = new LinkedHashSet<Statement>();//: Statements S u T
			statements.addAll(subgraf.getVertices());
			statements.addAll(subgraf.getEdges());
			
			Set<Statement> entryStatements = new LinkedHashSet<Statement>();
			entryStatements.addAll(subgraf.getInitialVertices());
			//TODO TEST
			entryStatements.addAll(subgraf.getActivationLinkVertices());
			//TSET
			
			//set abstractEnvMap to bottom
			for (Statement n : statements) {
				Abstract1 abstractEnvN = new Abstract1(man, env, true);
				abstractEnvMap.put(n, abstractEnvN);
			}
			//set abstractEnv(n) for entry statement to [0, 0]
			Interval[] initialBox = new Interval[varNames.length];
			for (Statement n : entryStatements) {
				for (int i = 0; i < initialBox.length; i++) {
					initialBox[i] = new Interval(0, 0);
				}
				Abstract1 absN = new Abstract1(man, env, varNames, initialBox);
				abstractEnvMap.get(n).join(man, absN);
			}
			//initialize worklist
			worklist.addAll(entryStatements);

			out += "\n" + printToBox("after initialization");

			while (!worklist.isEmpty()) {
				Statement statement = worklist.pollFirst();
				statement.increaseVisited();
				Abstract1 e = TransferFunction.transfer(statement, abstractEnvMap.get(statement), man, env);
				for (Statement downstream : getDownstream(statement, subgraf)) {
					if (!e.isIncluded(man, abstractEnvMap.get(downstream))) {
						if(statement.getVisited() > 5) {
							out += "\n" + "widening ";
							abstractEnvMap.get(downstream).join(man, abstractEnvMap.get(downstream).widening(man, e));
							//reset other widening-counters to make analysis more precise:
							for (Statement s : statements) {
								s.resetVisited();
							}
						} else {
							abstractEnvMap.get(downstream).join(man, e);
						}
						worklist.add(downstream);
						out += "\n" + printToBox("in progress - Step or tran = " + statement.getId());
					}
				}
			}
			
			//sysout
			out += "\n" + printToBox("termination - result");
			out += "\n" + "\n\n ----";
		}
		return out;
	}
	
	private String printToBox(String msg) {
		String out = "";
		
		out += "\n===== " + msg + " =====";
		out += "\n" + Arrays.toString(env.getVars());
		
		for (Statement n : this.abstractEnvMap.keySet()) {
			try {
				String nType = "";
				if(n instanceof Vertex) {
					nType = "Step ";
				} else {
					nType = "Tran ";
				}
				Abstract1 abstr1 = abstractEnvMap.get(n);
				Interval[] box = abstr1.toBox(man);
				out += "\n" + nType + n.getId() + ": " + Arrays.toString(box);
			} catch (ApronException e) {
				e.printStackTrace();
			}
		}
		return out;
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
	public Map<Statement, Abstract1> getAbstractEnvMap() {
		return abstractEnvMap;
	}
	public Manager getMan() {
		return man;
	}
	public Hypergraf getHypergraf() {
		return hypergraf;
	}
	
}
