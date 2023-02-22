package de.hsu.grafcet.staticAnalysis.abstInterpretation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import apron.Abstract1;
import apron.ApronException;
import apron.Environment;
import apron.Interval;
import apron.Manager;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.HierarchyDependency;
import de.hsu.grafcet.staticAnalysis.hypergraf.Statement;
import de.hsu.grafcet.staticAnalysis.hypergraf.Subgraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Vertex;

public class Util {

	public static Map<Statement, Abstract1> deepcopyEnvMap	(Manager man, Map<Statement, Abstract1> abstractEnvMap) throws ApronException{
		Map<Statement, Abstract1> copyAbstractEnvMap = new HashMap<Statement, Abstract1>();
		for (Statement s : abstractEnvMap.keySet()) {
			copyAbstractEnvMap.put(s, new Abstract1(man, abstractEnvMap.get(s)));
		}
		return copyAbstractEnvMap;		
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
			throw new IllegalArgumentException("Different statements in abstractEnvMaps. Comparing is not possible");
		}
	}
	public static boolean equalsInterface (Manager man, 
			Map<HierarchyDependency, Map<Statement, Abstract1>> interface1, Map<HierarchyDependency, Map<Statement, Abstract1>> interface2) 
			throws ApronException {
		if (interface1.keySet().containsAll(interface2.keySet()) && 
				interface2.keySet().containsAll(interface1.keySet())) {
			boolean isEaqual = true;
			for (HierarchyDependency d : interface1.keySet()) {
				if (!equalsEnvMaps(man, interface1.get(d), interface1.get(d))) {
					isEaqual = false;
				}
			}
			return isEaqual;
		}else {
			throw new IllegalArgumentException("Different statements in interfaces. Comparing is not possible");
		}
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
						nType = "Step ";
					} else {
						nType = "Tran ";
					}
					Abstract1 abstr1 = abstractEnvMap.get(n);
					Interval[] box = abstr1.toBox(man);
					out += "\n" + nType + n.getId() + ": \t" + printArray(box, i*rowLength, rowLength);
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
	
	
}
