package net.dflmngr.model.service;

import java.util.List;

import net.dflmngr.model.entity.DflRoundInfo;

public interface DflRoundInfoService extends GenericService<DflRoundInfo, Integer> {
	public List<DflRoundInfo> getRoundsByAflRounds(List<Integer> aflRounds);
}
