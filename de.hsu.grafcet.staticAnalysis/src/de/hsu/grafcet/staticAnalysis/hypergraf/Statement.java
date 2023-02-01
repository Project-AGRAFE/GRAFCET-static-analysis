package de.hsu.grafcet.staticAnalysis.hypergraf;

/**
 * 
 * @author aron
 * Statement in the control flow of GRAFCET. Implemented by Edge (Transition) and Vertex (Step)
 *
 */

public interface Statement {

	int getId();
	void increaseVisited();
	int getVisited();
	void resetVisited();

}
