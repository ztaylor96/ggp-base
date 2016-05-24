package notabot.gametree;

import java.util.List;

import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class BeliefTreeEdge {

	protected final BeliefStateTree bst;
	BeliefTreeNode prevNode;
	BeliefTreeNode nextNode;
	List<Move> jointMove;
	private double prob = 1;

	public BeliefTreeEdge(BeliefStateTree bst, BeliefTreeNode prevNode, List<Move> jointMove) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		this.bst = bst;
		this.jointMove = jointMove;
		this.prevNode = prevNode;
		nextNode = new BeliefTreeNode(bst, this, bst.iism.getNextState(prevNode.getState(), jointMove));
		nextNode.setDepth(prevNode.getDepth() + 1);
	}

	public BeliefTreeNode getPrevNode() {
		return prevNode;
	}

	public BeliefTreeNode getNextNode() {
		return nextNode;
	}

	public List<Move> getJointMove() {
		return jointMove;
	}

	public void setProbability(double prob) {
		this.prob = prob;
	}

	public double getProbability() {
		return prob;
	}
}
