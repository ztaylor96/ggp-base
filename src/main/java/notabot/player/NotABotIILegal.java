package notabot.player;

import java.util.List;

import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class NotABotIILegal extends NotABotII {

	@Override
	protected void runMetaGame() {
		// do nothing
	}

	@Override
	protected Move getBestMove(int turnNumber) throws MoveDefinitionException,
			TransitionDefinitionException, GoalDefinitionException {
		while (!NotABotII.hasTimedOut()) {}
		List<Move> moves;
		if (turnNumber == 1){
			moves = getMoves(getIIStateMachine().getInitialState());
		} else {
			moves = getMoves();
		}
		return moves.get(0);
	}

}
