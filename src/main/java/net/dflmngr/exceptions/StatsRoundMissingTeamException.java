package net.dflmngr.exceptions;

public class StatsRoundMissingTeamException extends RuntimeException {
    private static final String MSG = "Error finding stats roound team for TeamId: %s, dflRound: %d, aflRound: %d";
    
    public StatsRoundMissingTeamException(String teamId, int dflRound, int aflRound) {
        super(String.format(MSG, teamId, dflRound, aflRound));
    }
}
