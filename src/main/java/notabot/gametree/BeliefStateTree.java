package notabot.gametree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.SeesState;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.iistatemachine.IIStateMachine;

public class BeliefStateTree {

	protected volatile IIStateMachine iism;
	protected BeliefTreeNode root;

	// info for GameTreeNodes to access
	final Random rand = new Random();
	final List<Role> roles;
	final int playerIndex;// this player's role index
	// final MoveComparator moveComparator; //used to sort move lists
	int numDepthCharges = 0;// used to count number of paths sampled during turn
	private List<SeesState> seesHistory;
	private Set<BeliefTreeNode> possibleNodes;

	public BeliefStateTree(IIStateMachine stateMachine, Role playerRole) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		iism = stateMachine;
		roles = iism.getRoles();
		playerIndex = roles.indexOf(playerRole);
		root = new BeliefTreeNode(this, null, iism.getInitialState());
		root.setDepth(0);
		root.mark(1);
		root.buildSuccessors();
		seesHistory = new ArrayList<SeesState>();
		possibleNodes = new HashSet<BeliefTreeNode>();
	}

	public void addSuccessors() throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		for (BeliefTreeNode node : possibleNodes) {
			node.buildSuccessors();
		}
	}

	public boolean extendTreeToDepth(int depth) {
		return extendToDepth(root, depth);
	}

	private boolean extendToDepth(BeliefTreeNode node, int depth) {
		// base case
		if (node.getDepth() == depth) {
			if (node.getValue() == 0) {
				return false;
			} else if (node.getValue() == 1) {
				System.out.println("POSSIBLE NODE FOR TURN " + (depth+1) + ": " + node.getState().toString());
				return true;
			} else {
				System.out.println("BeliefStateTree: node at current depth unevaluated");
				return false;
			}
		}
		// recursive case
		boolean foundPossibleState = false;
		for (int i = 0; i < node.getChildEdges().length; i++) {	// for each child
			if (node.getChildEdges()[i].getNextNode().getValue() == -1) {	// if unknown, mark it
				node.markSuccessor(i, seesHistory.get(node.getDepth()));
			}
			if (node.getChildEdges()[i].getNextNode().getValue() == 1) {	// if possible, recurse on children
				boolean extended = extendToDepth(node.getChildEdges()[i].getNextNode(), depth);
				if (extended) foundPossibleState = true;
			}
		}
		return foundPossibleState;
	}

	public Map<BeliefTreeNode, Double> getPossibleNodes(BeliefTreeNode node, double runningProb, int depth) {
		Map<BeliefTreeNode, Double> possibleNodes = new HashMap<BeliefTreeNode, Double>();
		if (node.getDepth() == depth && node.getValue() == 1) {	// if desired depth, return this node
			possibleNodes.put(node, runningProb);
		} else if (!node.isTerminal) {	// not sure if this is necessary
			for (int i = 0; i < node.getChildEdges().length; i++) {
				if (node.getChildEdges()[i].getNextNode().getValue() == 1) {
					possibleNodes.putAll(getPossibleNodes(node.getChildEdges()[i].getNextNode(), runningProb * node.getChildEdges()[i].getProbability(), depth));
				}
			}
		}
		this.possibleNodes = possibleNodes.keySet();
		return possibleNodes;
	}

	public void appendSees(SeesState seesState) {
		seesHistory.add(seesState);
	}

	public BeliefTreeNode getRoot() {
		return root;
	}

}
