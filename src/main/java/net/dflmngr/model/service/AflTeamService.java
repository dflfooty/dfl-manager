package net.dflmngr.model.service;

import net.dflmngr.model.entity.AflTeam;

public interface AflTeamService extends GenericService<AflTeam, String> {
    AflTeam getAflTeamByName(String name);
}
