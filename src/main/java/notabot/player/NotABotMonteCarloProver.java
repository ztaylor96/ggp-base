package notabot.player;


import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public class NotABotMonteCarloProver extends NotABotMonteCarlo {

	@Override
	public StateMachine getInitialStateMachine() {
		//return new CachedStateMachine(new NotABotPropNetStateMachine());
		return new CachedStateMachine(new ProverStateMachine());
	}
}
