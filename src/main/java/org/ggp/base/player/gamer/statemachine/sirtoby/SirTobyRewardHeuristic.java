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


public final class SirTobyRewardHeuristic extends StateMachineGamer
{
	private int nodesVisited = 0;
	private int minScoresCalled = 0;
	private int maxScoresCalled = 0;

	@Override
	public String getName() {
		return "SirTobyReward";
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		System.out.println("\n\nSelecting move.");
		nodesVisited = 0;
		minScoresCalled = 0;
		maxScoresCalled = 0;
		long start = System.currentTimeMillis();

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		long buffer = (long) ((timeout - start) * 0.10); // use 90% of available time
		//System.out.println("buffer: " + buffer);
		Move selection = bestMove(getRole(), getCurrentState(), timeout - buffer);

		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		System.out.println("Num nodes visited: " + nodesVisited);
		System.out.println("Minscores called: " + minScoresCalled);
		System.out.println("Maxscores called: " + maxScoresCalled);
		System.out.println("Time used: " + (stop-start));
		return selection;
	}

	private Move bestMove(Role role, MachineState state, long end) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		List<Move> actions = getStateMachine().getLegalMoves(state, role);
		Move action = actions.get(0);
		int score = 0;
		if (getStateMachine().getLegalMoves(state, role).size() == 1) {
			nodesVisited += 1;	// pretend we visited the 1 legal node then return
			System.out.println("only one legal move, returning now");
			return getStateMachine().getLegalMoves(state, role).get(0);
		}

		List<Move> moves = getStateMachine().getLegalMoves(state, role);
		long start = System.currentTimeMillis();
		long time_per_branch = (end - start) / moves.size();
		int i = 1;
		for (Move move: moves) {
			int result;
			long end_time = start + time_per_branch*i;
			if (getStateMachine().getRoles().size() == 1) {
				nodesVisited += 1;
				MachineState nextState = getStateMachine().findNext(Arrays.asList(move), state);
				result = maxscore(role, nextState, end_time);
			} else {
				result = minscore(role, move, state, end_time);
			}

	        if (result == 100) { return move; }
	        if (result > score) {
	        	score = result;
	        	action = move;
	        }
	        i++;
	    }
		System.out.println("Score: " + score);
	    return action;
	}

	private int minscore(Role role, Move action, MachineState state, long end) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		minScoresCalled += 1;
		int score = 100;
		List<List<Move>> allJointMoves = getStateMachine().getLegalJointMoves(state, role, action);
		long start = System.currentTimeMillis();
		long time_per_branch = (end - start) / allJointMoves.size();
		nodesVisited += 1;	// count the first move (action parameter) as a jump to another node
		for (int i = 0; i < allJointMoves.size(); i++) {
			List<Move> moveSequence = allJointMoves.get(i);

			MachineState nextState = getStateMachine().findNext(moveSequence, state);
			nodesVisited += (moveSequence.size()-1); // count all the moves in the sequence as a node except first move,
													// which is same for every branch this case
			int result = maxscore(role, nextState, start + time_per_branch*(i+1));
			if (result == 0) { return 0; }
			score = Math.min(score, result);
			i++;
		}
		return score;
	}

	private Role findOpponent(Role role) {
		StateMachine sm = getStateMachine();
		int numRoles = sm.getRoles().size();
		Role opponent = sm.getRoles().get((sm.getRoleIndices().get(role) + 1) % numRoles);
		return opponent;
	}

	private int maxscore(Role role, MachineState state, long end) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		maxScoresCalled += 1;
		if (getStateMachine().findTerminalp(state)) {
			return getStateMachine().findReward(role,state);
		}
	    int score = 0;
	    List<Move> moves = getStateMachine().getLegalMoves(state, role);
	    long time_per_branch = (end - System.currentTimeMillis()) / moves.size();
	    for (int i = 0; i < moves.size(); i++) {
	    	Move move = moves.get(i);
	    	if (System.currentTimeMillis() >= end) {
	    		// no more time -- use heuristic to evaluate this state since we
	    		// couldn't do an exhaustive search
	    		score = Math.max(score, getHeuristicScore(role, state));
	    		break;
	    	}

	    	int result;
	    	if (getStateMachine().getRoles().size() == 1) {
	    		nodesVisited += 1;
				MachineState nextState = getStateMachine().findNext(Arrays.asList(move), state);
				result = maxscore(role, nextState, end + time_per_branch*(i+1));
			} else {
				result = minscore(role, move, state, end + time_per_branch*(i+1));
			}
	    	if (result == 100) { return result; }
	    	score = Math.max(score, result);
	    }
	    return score;
	 }

	private int getHeuristicScore(Role role, MachineState state) throws MoveDefinitionException, GoalDefinitionException {
		return getStateMachine().findReward(role, state);
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