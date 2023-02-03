package de.hsu.grafcet.staticAnalysis.abstInterpretation;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import apron.Abstract1;
import apron.ApronException;
import de.hsu.grafcet.staticAnalysis.hypergraf.Edge;
import de.hsu.grafcet.staticAnalysis.hypergraf.Subgraf;
import de.hsu.grafcet.staticAnalysis.hypergraf.Vertex;

public class DeadlockDetecter {
	
	
	public static String checkDeadlocks (SequAbstractInterpreter ai) throws ApronException{
		String out = "Abstract interpretation based deadlock detection:";
		boolean deadlockGlobal = false;
		for (Subgraf subgraf : ai.getHypergraf().getSubgrafs()) {
			boolean deadlockSubgraf = false;
			String temp = "Partial Grafcet: " + subgraf.getPartialGrafcet().getName();
			for (Vertex vertex : subgraf.getVertices()) {
				List<Edge> downstreamEdges = new ArrayList<Edge>(subgraf.getDownstreamEdges(vertex));
				//all abstract values of downstreamEdges are the same:
				Abstract1 absValueN = ai.getAbstractEnvMap().get(downstreamEdges.get(0));
				System.out.println(Arrays.toString(absValueN.toBox(ai.getMan())));
				Abstract1 absValueConditions = new Abstract1(ai.getMan(), ai.getEnv(), true);
				System.out.println(Arrays.toString(absValueConditions.toBox(ai.getMan())));
				for(Edge edge : downstreamEdges) {
					//using transfer function with top abstract values
					Abstract1 absValueCondition = TransferFunction.transfer(
							edge, new Abstract1(ai.getMan(),  ai.getEnv()), ai.getMan(), ai.getEnv());
					absValueConditions.join(ai.getMan(), absValueCondition);
				}
				System.out.println(Arrays.toString(absValueConditions.toBox(ai.getMan())));
				if(absValueN.isIncluded(ai.getMan(), absValueConditions)){
					//no deadlock detected
				} else {
					//deadlock detected
					deadlockSubgraf = true;
					deadlockGlobal = true;
					temp += "\n\nDeadlock detected regarding transition(s): " + downstreamEdges;
					temp += "\nAbstract values of variables: " + Arrays.toString(absValueN.toBox(ai.getMan()));
					temp += "\nAbstract values of associated transition conditions: " 
							+ Arrays.toString(absValueConditions.toBox(ai.getMan()));
				}
			}
			if (deadlockSubgraf) out += temp;
		}
		if(deadlockGlobal)
			return out;
		else
			return "No Deadlocks detected based onbstract interpretation";
	}
	
	
	
}
