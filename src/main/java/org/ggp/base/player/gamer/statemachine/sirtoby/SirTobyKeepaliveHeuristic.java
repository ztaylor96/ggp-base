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


public final class SirTobyKeepaliveHeuristic extends StateMachineGamer
{
	private int nodesVisited = 0;
	private int bestScore = 0;
	private Move bestMove = null;

	@Override
	public String getName() {
		return "SirTobyAlive";
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		nodesVisited = 0;
		bestScore = 0;
		bestMove = null;

		long start = System.currentTimeMillis();
		long buffer = (long) ((timeout - start) * 0.1);	// use 90% of available time
		long end = timeout - buffer;

		int depth = 2;
		bestMove(getRole(), getCurrentState(), end, depth);
		while (true) {
			depth += 1;
			System.out.println("Starting depth " + depth);
			bestMove(getRole(), getCurrentState(), timeout - buffer, depth);
			if (bestScore == 100) { break; }
			if (System.currentTimeMillis() >= end) { break; }
			if (depth >= 10) { break; }
		}
		long stop = System.currentTimeMillis();

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		notifyObservers(new GamerSelectedMoveEvent(moves, bestMove, stop - start));
		System.out.println("Num nodes visited: " + nodesVisited);
		System.out.println("Time used: " + (stop-start));
		return bestMove;
	}

	private Move bestMove(Role role, MachineState state, long end, int limit) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		List<Move> actions = getStateMachine().getLegalMoves(state, role);
		if (bestMove == null) {
			bestMove = actions.get(0);
		}

		if (actions.size() == 1) {
			nodesVisited += 1;	// visit the only legal move node
			return bestMove;
		}

		boolean singlePlayer = (getStateMachine().getRoles().size() == 1);
		for (Move move: actions) {
			nodesVisited += 1;	// visit node that comes from this move
			int result;
			if (singlePlayer) {
				List<Move> choice = Arrays.asList(move);
				result = singlePlayerMaxscore(role, getStateMachine().findNext(choice, state), 1, limit, end);
			} else {
				result = minscore(role, move, state, 0, limit, end);
			}

	        if (result > bestScore) {
	        	bestScore = result;
	        	bestMove = move;
	        	System.out.println("new best score: " + bestScore);
	        	System.out.println("new best action: " + bestMove);
	        }
	        if (bestScore == 100) { System.out.println("**** found a perfect move: " + bestMove); break;}
	        if (System.currentTimeMillis() >= end) { break; }
	    }

	    return bestMove;
	}

	private int minscore(Role role, Move action, MachineState state, int level, int limit, long end) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		if (getStateMachine().findTerminalp(state)) { return getStateMachine().findReward(role,state); }

		if (level >= limit || System.currentTimeMillis() >= end) {
			return getHeuristicScore(role, state);
		}

		int score = 100;
		List<List<Move>> allJointMoves = getStateMachine().getLegalJointMoves(state, role, action);
		for (List<Move> moveSequence: allJointMoves) {
			MachineState nextState = getStateMachine().findNext(moveSequence, state);
			int result = maxscore(role, nextState, level + 1, limit, end);
			if (result == 0) { return 0; }
			score = Math.min(score, result);
		}
		return score;
	}

	private int maxscore(Role role, MachineState state, int level, int limit, long end) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		nodesVisited += 1;
		if (getStateMachine().findTerminalp(state)) { return getStateMachine().findReward(role,state); }
		if (level >= limit || System.currentTimeMillis() >= end) { return getHeuristicScore(role, state); }

	    int score = 0;
	    for (Move action: getStateMachine().getLegalMoves(state, role)){
	    	int result = minscore(role, action, state, level, limit, end);
	    	if (result == 100) { return result; }
	    	score = Math.max(score, result);
	    }

	    return score;
	 }

	private int singlePlayerMaxscore(Role role, MachineState state, int level, int limit, long end) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		if (getStateMachine().findTerminalp(state)) { return getStateMachine().findReward(role,state); }
		if (level >= limit || System.currentTimeMillis() >= end) { return getHeuristicScore(role, state); }

		List<Move> actions = getStateMachine().getLegalMoves(state, role);
	    int score = 0;
	    for (int i = 0; i < actions.size(); i++){
	    	nodesVisited += 1;	// visit move that comes from taking this action
	    	List<Move> singleActionList = Arrays.asList(actions.get(i));
	    	int result = singlePlayerMaxscore(role, getStateMachine().findNext(singleActionList, state), level + 1, limit, end);
	    	if (result == 100) { System.out.println("*** found winning state"); return result; }
	    	score = Math.max(score, result);
	    }

	    return score;
	 }

	private int getHeuristicScore(Role role, MachineState state) throws MoveDefinitionException, GoalDefinitionException {
		if (getStateMachine().findTerminalp(state)) {
			return getStateMachine().findReward(role,state);
		}
		return Math.min(99, getStateMachine().findReward(role,state) + 1);
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