package notabot.gametree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.IIPropNet;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.SeesState;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.iistatemachine.NotABotIIPropNetStateMachine;


public class BeliefTreeNode {

	protected final BeliefStateTree bst;
	MachineState state;
	private final Role role;
	private int value = -1; // 1 if possible, 0 if impossible, -1 if unknown
	boolean isTerminal;
	int terminalGoal = 0;
	int numMoveCombos;
	BeliefTreeEdge[] childEdges;
	//Set<BeliefTreeNode> children;
	BeliefTreeEdge parentEdge;
	List<Move> playerMoves;
	int numPlayerMoves;
	private int depth;

	public BeliefTreeNode(BeliefStateTree bst, BeliefTreeEdge parent, MachineState state) throws GoalDefinitionException, MoveDefinitionException {
		this.bst = bst;
		this.parentEdge = parent;
		role = bst.iism.getRoles().get(bst.playerIndex);
		constructBeliefTreeNode(state);
	}

	private void constructBeliefTreeNode(MachineState state) throws GoalDefinitionException, MoveDefinitionException {
		playerMoves = bst.iism.getLegalMoves(state, role);
		numPlayerMoves = playerMoves.size();
		this.state = state;
		isTerminal = bst.iism.isTerminal(state);
		if (isTerminal) {
			terminalGoal = bst.iism.getGoal(state, bst.iism.getRoles().get(bst.playerIndex));
		} else {
			numMoveCombos = 1;
			for (Role role: bst.iism.getRoles()){
				List<Move> currMoves = bst.iism.getLegalMoves(state, role);
				numMoveCombos *= currMoves.size();
			}
			childEdges = new BeliefTreeEdge[numMoveCombos];
		}
	}

	public List<Move> computeJointMove(int combo) {
		try {
			// compute move combo
			List<Move> moveCombo = new ArrayList<Move>();
			int divisor = numPlayerMoves;
			int playerMoveIndex = combo%numPlayerMoves;
			// get each opponent's move for current combination
			for (int r = 0; r < bst.roles.size(); r++) {
				if (r != bst.playerIndex){
					List<Move> currMoves = bst.iism.getLegalMoves(state, bst.roles.get(r));
					moveCombo.add(currMoves.get((combo/divisor) % currMoves.size()));
					divisor *= currMoves.size();
				}
				else{
					moveCombo.add(playerMoves.get(playerMoveIndex));
				}
			}
			return moveCombo;

		}
		catch (MoveDefinitionException e) {
			e.printStackTrace();
			return null;
		}
	}


	public void makeChildEdge(int combo) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		List<Move> jointMove = computeJointMove(combo);
		childEdges[combo] = new BeliefTreeEdge(bst, this, jointMove);
	}

	public void buildSuccessors() throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
		for (int i = 0; i < numMoveCombos; i++) {
			makeChildEdge(i);
		}
	}

	public void markSuccessor(int combo, SeesState seesState) {
		MachineState state = childEdges[combo].getNextNode().getState();

		if (seesState.getSees().equals(bst.iism.getSeesState(state, seesState.getRole()).getSees())) {
			boolean basesConflict = false;
			Map<GdlSentence, Proposition> baseProps = bst.iism.getIIPropNet().getBasePropositions();
			Map<Proposition, Double> knownProps = ((NotABotIIPropNetStateMachine)(bst.iism)).getKnownProps();
			if (knownProps != null && knownProps.size() > 0) {
				IIPropNet iipropNet = bst.iism.getIIPropNet();
				for (GdlSentence sent : baseProps.keySet()) {
					if (state.getContents().contains(sent) && knownProps.keySet().contains(baseProps.get(sent)) && (knownProps.get(baseProps.get(sent)) == 0)) {
						childEdges[combo].getNextNode().mark(0);
						basesConflict = true;
						break;
					} else if (!state.getContents().contains(sent) && knownProps.keySet().contains(baseProps.get(sent)) && (knownProps.get(baseProps.get(sent)) == 1)) {
						childEdges[combo].getNextNode().mark(0);
						basesConflict = true;
						break;
					}
				}
			}
			if (!basesConflict) childEdges[combo].getNextNode().mark(1);
		} else {
			childEdges[combo].getNextNode().mark(0);
		}
	}

	public void mark(int value) {
		this.value = value;
		if (value == 0) {
			for (int i = 0; i < numMoveCombos; i++) {
				if (childEdges[i] != null) {
					childEdges[i].getNextNode().mark(0);
				}
			}
		}
	}

	public BeliefTreeEdge[] getChildEdges() {
		return childEdges;
	}

	public int getValue() {
		return value;
	}

	public MachineState getState() {
		return state;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}
}
