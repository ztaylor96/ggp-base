package org.ggp.base.player.gamer.statemachine.sirtoby;

import java.util.List;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;


public final class SirtobyAlphaBeta extends StateMachineGamer
{
	@Override
	public String getName() {
		return "SirtobyAlphaBeta";
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		long start = System.currentTimeMillis();

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move selection = bestMove(getRole(), getCurrentState());

		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	private Move bestMove(Role role, MachineState state) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		List<Move> actions = getStateMachine().getLegalMoves(state, role);
		Move action = actions.get(0);
		int score = 0;
		int[] alpha = new int[1];
		int[] beta = new int[1];
		alpha[0] = 0;
		beta[0] = 100;
		//System.out.println(getStateMachine().getLegalMoves(state, role).toString());
		for (Move move: getStateMachine().getLegalMoves(state, role)) {
			//System.out.println("exploring move: " + move.toString());
			int result = minscore(role, move, state, alpha, beta);
			//System.out.println("got score: " + result);
	        if (result == 100) { return move; }
	        if (result > score) {
	        	score = result;
	        	action = move;
	        }
	    }
	    return action;
	}

	private int minscore(Role role, Move action, MachineState state, int[] alpha, int[] beta) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		int score = 100;
		List<List<Move>> allJointMoves = getStateMachine().getLegalJointMoves(state, role, action);
//		int index = (getStateMachine().getRoleIndices().get(role) + 1) % getStateMachine().getRoles().size();
//		Role opp = getStateMachine().getRoles().get(index);
//		for (Move move: getStateMachine().getLegalMoves(state, opp)) {
//			List<Move> moves = new ArrayList<Move>();
//			if (role == getStateMachine().getRoles().get(0)) {
//				moves.add(action);
//				moves.add(move);
//			} else {
//				moves.add(move);
//				moves.add(action);
//			}
//			MachineState simulatedNextState = getStateMachine().findNext(moves, state);
//			int result = maxscore(role, simulatedNextState);
////			if (result == 0) { return 0; }
//			if (result < score) { score = result; }
//		}
		//System.out.println(allJointMoves.toString());
		//System.out.println("\n");
		for (List<Move> moveSequence: allJointMoves) {
			MachineState simulatedNextState = getStateMachine().findNext(moveSequence, state);
			int result = maxscore(role, simulatedNextState, alpha, beta);
			if (result <= alpha[0]) { return result; }
			beta[0] = Math.min(beta[0], result);
			if (result == 0) { return 0; }
			if (result < score) { score = result; }
		}
		return score;
	}

	private int maxscore(Role role, MachineState state, int[] alpha, int[] beta) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		if (getStateMachine().findTerminalp(state)) {
			return getStateMachine().findReward(role,state);
		}

	    int score = 0;
	    for (Move action: getStateMachine().getLegalMoves(state, role)){
	    	int result = minscore(role, action, state, alpha, beta);
	    	if (result >= beta[0]) { return result; }
	    	alpha[0] = Math.max(result, alpha[0]);
	    	if (result == 100) { return result; }
	    	if (result > score) { score = result; }
	    }
	    return score;
	 }


	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// Random gamer does no game previewing.
	}

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// Random gamer does no metagaming at the beginning of the match.
	}

	@Override
	public void stateMachineStop() {
		// Random gamer does no special cleanup when the match ends normally.
	}

	@Override
	public void stateMachineAbort() {
		// Random gamer does no special cleanup when the match ends abruptly.
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
	}
}