package notabot.player;

import notabot.NotABot;

import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

/**
 * Always plays the first move in the list
 */
public class NotABotLegal extends NotABot {

	@Override
	protected void runMetaGame() {
		// do nothing
	}

	@Override
	protected Move getBestMove() throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		return getMoves().get(0);
	}

}
