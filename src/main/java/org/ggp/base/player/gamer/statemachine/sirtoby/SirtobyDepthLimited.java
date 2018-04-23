package org.ggp.base.player.gamer.statemachine.sirtoby;

import java.util.Arrays;
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


public final class SirtobyDepthLimited extends StateMachineGamer
{
	private int nodesVisited = 0;
	private int LIMIT = 6;

	@Override
	public String getName() {
		return "SirtobyDepthLimited";
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
		nodesVisited += 1;

		List<Move> actions = getStateMachine().getLegalMoves(state, role);
		Move action = actions.get(0);
		int score = 0;

		//System.out.println(getStateMachine().getLegalMoves(state, role).toString());
		if (getStateMachine().getLegalMoves(state, role).size() == 1) {
			return getStateMachine().getLegalMoves(state, role).get(0);
		}

		boolean singlePlayer = (getStateMachine().getRoles().size() == 1);

		for (Move move: getStateMachine().getLegalMoves(state, role)) {
			int result;
			if (singlePlayer) {
				LIMIT = 8;
				result = singlePlayerMaxscore(role, state, 0);
			} else {
				result = minscore(role, move, state, 0);
			}
			//System.out.println("exploring move: " + move.toString());
	        if (result == 100) { return move; }
	        //System.out.println("got minscore: " + result);
	        if (result > score) {
	        	score = result;
	        	action = move;
	        }
	    }
		System.out.println("nodes visited" + nodesVisited);
	    return action;
	}

	private int minscore(Role role, Move action, MachineState state, int level) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		nodesVisited += 1;

		System.out.println("Level " + level);
		if (level >= LIMIT) {
			System.out.println("Returning");
			return 0;
		}

		int score = 100;
		List<List<Move>> allJointMoves = getStateMachine().getLegalJointMoves(state, role, action);
		for (List<Move> moveSequence: allJointMoves) {
			MachineState simulatedNextState = getStateMachine().findNext(moveSequence, state);
			int result = maxscore(role, simulatedNextState, level + 1);
			if (result == 0) { return 0; }
			if (result < score) { score = result; }
		}
		return score;
	}

	private int maxscore(Role role, MachineState state, int level) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		System.out.println("Level " + level);
		nodesVisited += 1;
		if (getStateMachine().findTerminalp(state)) {
			return getStateMachine().findReward(role,state);
		}

		if (level >= LIMIT) {
			System.out.println("Returning");
			return 0;
		}

	    int score = 0;
	    for (Move action: getStateMachine().getLegalMoves(state, role)){
	    	int result = minscore(role, action, state, level);
	    	if (result == 100) { return result; }
	    	if (result > score) { score = result; }
	    }
	    return score;
	 }

	private int singlePlayerMaxscore(Role role, MachineState state, int level) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{

		if (getStateMachine().findTerminalp(state)) {
			return getStateMachine().findReward(role,state);
		}
		if (level >= LIMIT) {
			System.out.println("Returning");
			return 0;
		}
		List<Move> actions = getStateMachine().getLegalMoves(state, role);
	    int score = 0;
	    for (int i = 0; i < actions.size(); i++){
	    	List<Move> singleActionList = Arrays.asList(actions.get(i));
	    	int result = singlePlayerMaxscore(role, getStateMachine().findNext(singleActionList, state), level + 1);
	    	if (result > score) {
	        	score = result;
	        }
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