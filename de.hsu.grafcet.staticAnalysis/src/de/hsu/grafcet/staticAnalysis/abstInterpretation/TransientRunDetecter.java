package de.hsu.grafcet.staticAnalysis.abstInterpretation;

import java.util.HashSet;
import java.util.Set;

import apron.Abstract1;
import apron.ApronException;
import apron.Box;
import de.hsu.grafcet.staticAnalysis.hierarchyOrder.*;
import de.hsu.grafcet.staticAnalysis.hypergraf.*;

public class TransientRunDetecter extends Detecter{

	private Set<Vertex> transientVertices = new HashSet<Vertex>();
	
	public TransientRunDetecter(HierarchyOrder h) {
		super(h);
	}

	@Override
	public void runAnalysis() throws ApronException {
		for (Subgraf s : this.hierarchyOrder.getHypergraf().getSubgrafs()) {
			for (Vertex vertex : s.getVertices()) {
				for (Edge upstream : s.getUpstreamEdges(vertex)) {
					TransferFunction trans = new TransferFunction(
							null, new Abstract1(new Box(), hierarchyOrder.getEnv(), false),new Box(), hierarchyOrder.getEnv());
					Abstract1 abstractUpstream = trans.transferTerm(upstream.getTransition().getTerm());
					for (Edge downstream : s.getDownstreamEdges(vertex)) {
						Abstract1 abstractDownstream = trans.transferTerm(upstream.getTransition().getTerm());
						Abstract1 meetUD = abstractDownstream.meetCopy(new Box(), abstractUpstream);
						if (!meetUD.isBottom(new Box())){
							//Wertebereiche der Transitionsbedingungen sind nicht disjunkt
							//ACHTUNG das ist nicht sound hinsichtlich transienten Abläufen, da Aktionen die Wertebereiche während der Abläufe ändern können!
							transientVertices.add(vertex);
						}
					}
				}
			}
		}
	}

	@Override
	public String getResults() {
		String warning = "\n\n\n\n Detection of transient steps: \nCaution: Results might be unsound.\n";
		String out = "No transient steps detected.";
		if (!transientVertices.isEmpty()) {
			out = "The following steps might be transient: " + transientVertices.toString();
		}
		
		return warning + out;
	}
	
	

	
}
