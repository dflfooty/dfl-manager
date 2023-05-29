package net.dflmngr.model.entity.keys;

import java.io.Serializable;

public class StatsRoundPlayerStatsPK implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private int round;
	private String name;
	private String team;
	private int jumperNo;
	
	public int getRound() {
		return round;
	}

	public void setRound(int round) {
		this.round = round;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTeam() {
		return team;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public int getJumperNo() {
		return jumperNo;
	}

	public void setJumperNo(int jumperNo) {
		this.jumperNo = jumperNo;
	}

	@Override
	public String toString() {
		return "StatsRoundStatsPK [round=" + round + ", name=" + name + ", team=" + team + ", jumperNo=" + jumperNo
				+ "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + round;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((team == null) ? 0 : team.hashCode());
		result = prime * result + jumperNo;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
			StatsRoundPlayerStatsPK other = (StatsRoundPlayerStatsPK) obj;
		if (round != other.round)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (team == null) {
			if (other.team != null)
				return false;
		} else if (!team.equals(other.team))
			return false;
		if (jumperNo != other.jumperNo)
			return false;
		return true;
	}
}
