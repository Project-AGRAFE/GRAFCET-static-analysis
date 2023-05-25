package de.hsu.grafcet.staticAnalysis.abstInterpretation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apron.Abstract1;
import apron.ApronException;
import apron.Environment;
import apron.Interval;
import apron.Manager;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyDependency;
import de.hsu.grafcet.staticAnalysis.hypergraf.Edge;
import de.hsu.grafcet.staticAnalysis.hypergraf.Hypergraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Statement;
import de.hsu.grafcet.staticAnalysis.hypergraf.Subgraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Vertex;
import terms.VariableDeclaration;
import terms.VariableDeclarationType;

public class Util {

	public static Map<Statement, Abstract1> deepcopyEnvMap	(Manager man, Map<Statement, Abstract1> abstractEnvMap) throws ApronException{
		Map<Statement, Abstract1> copyAbstractEnvMap = new HashMap<Statement, Abstract1>();
		for (Statement s : abstractEnvMap.keySet()) {
			copyAbstractEnvMap.put(s, new Abstract1(man, abstractEnvMap.get(s)));
		}
		return copyAbstractEnvMap;		
	}
	
	public static Map<Statement, Abstract1> joinInterface(Manager man, 
			Map<Statement, Abstract1> abstInterface, Map<Statement, Abstract1> newInterface) throws ApronException{
		Map<Statement, Abstract1> abstInterfaceOut = new HashMap<Statement, Abstract1>();
		Set<Statement> visitedStatements = new HashSet<Statement>();
		for (Statement s : abstInterface.keySet()) {
			if (newInterface.containsKey(s)) {
				abstInterfaceOut.put(s, abstInterface.get(s).joinCopy(man, newInterface.get(s)));
				visitedStatements.add(s);
			}
		}
		Set<Statement> unvisitedStements = new HashSet<Statement>(newInterface.keySet());
		unvisitedStements.removeAll(visitedStatements);
		
		//Tritt dieser Fall überhaupt ein? Es werden ja nicht neue Statements in newInterface aufgenommen, da die Struktur des Grafcet gleich bleibt
		for (Statement s : unvisitedStements) {
			if (abstInterface.containsKey(s)) {
				throw new IllegalArgumentException("Error in algorithm");
			}
			abstInterfaceOut.put(s, newInterface.get(s));
		}
		return abstInterfaceOut;
	}
	
	public static Map<Statement, Abstract1> joinCopyEnvMaps	(Manager man, 
			Map<Statement, Abstract1> abstractEnvMap1, Map<Statement, Abstract1> abstractEnvMap2) 
			throws ApronException{
		if (abstractEnvMap1.keySet().containsAll(abstractEnvMap2.keySet()) && 
				abstractEnvMap2.keySet().containsAll(abstractEnvMap1.keySet())) {
			Map<Statement, Abstract1> abstractEnvMapOut = new HashMap<Statement, Abstract1>();
			for (Statement s : abstractEnvMap1.keySet()) {
				abstractEnvMapOut.put(s, abstractEnvMap1.get(s).joinCopy(man, abstractEnvMap2.get(s)));
			}
			return abstractEnvMapOut;	
		}else {
			throw new IllegalArgumentException("Different statements in abstractEnvMaps. Joining is not possible");
		}
	}
	
	public static boolean equalsEnvMaps (Manager man, 
			Map<Statement, Abstract1> abstractEnvMap1, Map<Statement, Abstract1> abstractEnvMap2) 
			throws ApronException {
		if (abstractEnvMap1.keySet().containsAll(abstractEnvMap2.keySet()) && 
				abstractEnvMap2.keySet().containsAll(abstractEnvMap1.keySet())) {
			boolean isEaqual = true;
			for (Statement s : abstractEnvMap1.keySet()) {
				if (!abstractEnvMap1.get(s).isEqual(man, abstractEnvMap2.get(s))) {
					isEaqual = false;
				}
			}
			return isEaqual;
		}else {
			//throw new IllegalArgumentException("Different statements in abstractEnvMaps. Comparing is not possible");
			return false;
		}
	}
	public static boolean equalsInterface (Manager man, 
			Map<HierarchyDependency, Map<Statement, Abstract1>> interface1, Map<HierarchyDependency, Map<Statement, Abstract1>> interface2) 
			throws ApronException {
		if (interface1.keySet().containsAll(interface2.keySet()) && 
				interface2.keySet().containsAll(interface1.keySet())) {
			boolean isEaqual = true;
			for (HierarchyDependency d : interface1.keySet()) {
				if (!equalsEnvMaps(man, interface1.get(d), interface2.get(d))) {
					isEaqual = false;
					return false;
				}
			}
			return isEaqual;
		}else {
			return false;
			//throw new IllegalArgumentException("Different statements in interfaces. Comparing is not possible");
		}
	}
	//TODO Mit null testen
	public static String printAbst (String caption, Abstract1 abstValue, Environment env, Manager man) {
		Map<Statement, Abstract1> m = new HashMap<Statement, Abstract1>();
		m.put(null, abstValue);
		return printAbstMap(caption, m, env, man);
	}
	
	public static String printAbstMap(String caption, Map<Statement, Abstract1> abstractEnvMap, Environment env, Manager man) {
		int rowLength = 18;
		String out = "";
		int j = env.getVars().length / rowLength;
		out += "\n===== " + caption + " =====";
		for(int i = 0; i <= j; i++) {
			out += "\n===== " + caption + " (" + i + ") =====";
			out += "\nVariables: \t" + printArray(env.getVars(), i*rowLength, rowLength);			
			for (Statement n : abstractEnvMap.keySet()) {
				try {
					String nType = "";
					if(n instanceof Vertex) {
						nType = "Step " + n.getId();
					} else if (n instanceof Edge){
						nType = "Tran " + n.getId();
					} else {
						nType = "Int   ";
					}
					Abstract1 abstr1 = abstractEnvMap.get(n);
					Interval[] box = abstr1.toBox(man);
					out += "\n" + nType + ": \t" + printArray(box, i*rowLength, rowLength);
				} catch (ApronException e) {
					e.printStackTrace();
				}
			}	
		}	
		return out;
	}
	
	private static String printArray(Object[] obj, int start, int n) {
		String out = "";
		int end = Math.min(start + n, obj.length);
		for (int i = start; i < end; i++) {
			out += obj[i] + "\t";
		}
		return out;
	}
	
	/**
	 * Returns a set of lists that contain two items of the provided set. All possible combinations are provided.
	 * @param <T>
	 * @param set
	 * @return
	 */
	public static <T> Set<List<T>> getSetOfPairs(Set<T> set){
		if (set.size() == 2) {
			return Collections.singleton(new ArrayList<>(set));
		} else if (set.size() < 2) {
			throw new IllegalArgumentException("set size is less than 2");
		}
		Set<List<T>> setOfPairs = new HashSet<List<T>>();
		Set<T> visited = new HashSet<T>();
		for (T t1 : set) {
			Set<T> remaining = new HashSet<T>(set);
			visited.add(t1);
			remaining.removeAll(visited);
			for (T t2 : remaining) {
				List<T> pair = new ArrayList<T>();
				pair.add(t1);
				pair.add(t2);
				setOfPairs.add(pair);
			}
		}
		return setOfPairs;
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
	
	public static String[] getInputVars(Hypergraf h) {
		Set<VariableDeclaration> variableDeclarationSet = new LinkedHashSet<VariableDeclaration>();
		for (VariableDeclaration var : h.getGlobalGrafcet().getVariableDeclarationContainer().getVariableDeclarations()) {
			//collect input variables
			if (var.getVariableDeclarationType().equals(VariableDeclarationType.INPUT)) {
				variableDeclarationSet.add(var);
			}
		}
		String[] varNames = new String[variableDeclarationSet.size()];
		int i = 0;
		for (VariableDeclaration var : variableDeclarationSet) {
			varNames[i] = var.getName();
			i++;
		}
		return varNames;
	}
	
	
}
