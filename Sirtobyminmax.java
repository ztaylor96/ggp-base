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

/**
 * RandomGamer is a very simple state-machine-based Gamer that will always
 * pick randomly from the legal moves it finds at any state in the game.
 */
public final class SirtobyCompdelibGamer extends StateMachineGamer
{
	@Override
	public String getName() {
		return "SirtobyCompdelib";
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
		for (int i = 0; i < actions.size(); i++) {
			List<Move> singleActionList = Arrays.asList(actions.get(i));
			int result = maxscore(role, getStateMachine().findNext(singleActionList, state));
	        if (result == 100) {
	        	return actions.get(i);
	        }
	        if (result > score) {
	        	score = result;
	        	action = actions.get(i);
	        }
	    }
	    return action;
	}

	private int maxscore (Role role, MachineState state) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		if (getStateMachine().findTerminalp(state)) {
			return getStateMachine().findReward(role,state);
		}
		List<Move> actions = getStateMachine().getLegalMoves(state, role);
	    int score = 0;
	    for (int i = 0; i < actions.size(); i++){
	    	List<Move> singleActionList = Arrays.asList(actions.get(i));
	    	int result = maxscore(role, getStateMachine().findNext(singleActionList, state));
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
