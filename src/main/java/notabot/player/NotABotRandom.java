package notabot.player;

import java.util.List;
import java.util.Random;

import notabot.NotABot;

import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

/**
 * Plays a random move in the list
 */
public class NotABotRandom extends NotABot{

	private final Random RAND = new Random();

	@Override
	protected void runMetaGame() {
		// do nothing
	}

	@Override
	protected Move getBestMove() throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		List<Move> moves = getMoves();
		return moves.get(RAND.nextInt(moves.size()));
	}

}
