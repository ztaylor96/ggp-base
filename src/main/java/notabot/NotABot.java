package notabot;

import java.util.List;

import notabot.propnet.NotABotPropNetStateMachine;

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

public abstract class NotABot extends StateMachineGamer{

	// time at which computation must stop
	private static long timeout;
	// time left that computation of a move or metagame must stop
	private static final long TIME_CUSHION = 1000;

	/**
	 * Run metagame before the game starts
	 */
	protected abstract void runMetaGame();

	/**
	 * @return the best move for the current state
	 */
	protected abstract Move getBestMove()
		throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException;

	/**
	 * @return true if the timer has sufficiently run out
	 */
	public static boolean hasTimedOut(){
		return NotABot.timeout < System.currentTimeMillis();
	}

	public static long timeLeft(){
		return Math.max(0, NotABot.timeout-System.currentTimeMillis());
	}

	/**
	 * Quick helper methods that return a list of moves
	 * By default, uses current state and this role
	 */
	protected List<Move> getMoves() throws MoveDefinitionException{
		return getMoves(getCurrentState(), getRole());
	}

	protected List<Move> getMoves(MachineState state) throws MoveDefinitionException{
		return getMoves(state, getRole());
	}

	protected List<Move> getMoves(Role role) throws MoveDefinitionException{
		return getMoves(getCurrentState(), role);
	}

	protected List<Move> getMoves(MachineState state, Role role) throws MoveDefinitionException{
		return getStateMachine().getLegalMoves(state, role);
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new NotABotPropNetStateMachine());
		//return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		// update time at which timeout will occur
		NotABot.timeout = timeout - TIME_CUSHION;

		// inheriting subclass will run metagame
		runMetaGame();
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		// update time at which timeout will occur
		this.timeout = timeout - TIME_CUSHION;

		// get best possible move
		Move move = getBestMove();

		System.out.println("TIME: " + (timeout - System.currentTimeMillis()) + " - Move: "+ move + "\n");

		return move;
	}

	@Override
	public void stateMachineStop() {
		// TODO Auto-generated method stub
	}

	@Override
	public void stateMachineAbort() {
		// TODO Auto-generated method stub
	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// TODO Auto-generated method stub
	}

	@Override
	public String getName() {
		// gets the name of the inheriting subclass
		return getClass().getSimpleName();
	}

}
