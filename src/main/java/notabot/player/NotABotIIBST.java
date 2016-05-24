package notabot.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import notabot.MoveScore;
import notabot.gametree.BeliefStateTree;
import notabot.gametree.BeliefTreeNode;
import notabot.gametree.GameTree;

import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.iistatemachine.NotABotIIPropNetStateMachine;

public class NotABotIIBST extends NotABotII {

	private BeliefStateTree bst;

	@Override
	protected void runMetaGame() throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		bst = new BeliefStateTree(getIIStateMachine(), getRole());

	}

	@Override
	protected Move getBestMove(int turnNumber)
			throws MoveDefinitionException, TransitionDefinitionException,
			GoalDefinitionException {
		List<Move> legalMoves;
		if (turnNumber == 1) {
			System.out.println("TURN #1:");
			legalMoves = getMoves(getIIStateMachine().getInitialState());
			return legalMoves.get(0);
		} else {
			legalMoves = getMoves();
			System.out.println();
			System.out.println("---------------------------------------\n");
			System.out.println("TURN #" + turnNumber + ":");
			System.out.println("CURRENT SEES: " + getCurrentSees().getSees().toString() + '\n');
			bst.appendSees(getCurrentSees());
			bst.addSuccessors();
			System.out.println("Derived true base propositions: ");
			for (Proposition p : ((NotABotIIPropNetStateMachine)getIIStateMachine()).getTrueProps()) {
				System.out.println(p.getName().toString());
			}
			System.out.println();
			bst.extendTreeToDepth(turnNumber-1);
			Map<BeliefTreeNode, Double> possibleNodes = bst.getPossibleNodes(bst.getRoot(), 1, turnNumber-1);
			Map<GameTree, BeliefTreeNode> trees = new HashMap<GameTree, BeliefTreeNode>();
			for (BeliefTreeNode node : possibleNodes.keySet()) {
				trees.put(new GameTree(getNormalStateMachine(), node.getState(), getRole()), node);
			}
			sampleUntilTimeout(trees);
			Map<MoveScore, GameTree> moveScores = getMoveScores(trees.keySet());
			List<MoveScore> adjustedScores = new ArrayList<MoveScore>();
			for (MoveScore ms : moveScores.keySet()) {
				double score = ms.getScore() * possibleNodes.get(trees.get(moveScores.get(ms)));
				adjustedScores.add(new MoveScore(score, ms.getMove()));
			}
			int index = getHighestLegalScore(adjustedScores);
			while (!legalMoves.contains(adjustedScores.get(index).getMove())) {
				adjustedScores.remove(index);
				index = getHighestLegalScore(adjustedScores);
			}
			return adjustedScores.get(index).getMove();
		}
	}

	private void sampleUntilTimeout(Map<GameTree, BeliefTreeNode> trees) {
		for (GameTree tree : trees.keySet()) {
			tree.resetDepthChargeCounter();
		}
		while (!NotABotII.hasTimedOut()) {
			for (GameTree tree : trees.keySet()) {
				tree.runSample();
			}
		}
		for (GameTree tree : trees.keySet()) {
			System.out.println();
			System.out.println("Game tree with root " + trees.get(tree).getState() + ":");
			System.out.println("Depth charges: " + tree.getNumDepthCharges());
		}
	}

	private Map<MoveScore, GameTree> getMoveScores(Set<GameTree> trees) {
		Map<MoveScore, GameTree> moveScores = new HashMap<MoveScore, GameTree>();
		for (GameTree tree : trees) {
			moveScores.put(tree.getBestMoveScore(false), tree);
		}
		return moveScores;
	}

	private int getHighestLegalScore(List<MoveScore> moveScores) {
		double maxScore = 0;
		int index = 0;
		for (int i = 0; i < moveScores.size(); i++) {
			if (moveScores.get(i).getScore() > maxScore) {
				maxScore = moveScores.get(i).getScore();
				index = i;
			}
		}
		return index;
	}

}
