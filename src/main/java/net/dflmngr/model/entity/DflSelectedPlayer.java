package net.dflmngr.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import net.dflmngr.model.entity.keys.DflSelectedPlayerPK;

@Entity
@Table(name="dfl_selected_player")
@IdClass(DflSelectedPlayerPK.class)
public class DflSelectedPlayer {
	
	@Id
	private int round;
	
	@Id
	@Column(name = "player_id")
	private int playerId;
	
	@Column(name = "team_player_id")
	private int teamPlayerId;
	
	@Column(name = "team_code")
	private String teamCode;
	
	@Column(name = "is_emergency")
	private int isEmergency;
	
	@Column(name = "is_dnp")
	private boolean isDnp;
	
	@Column(name = "score_used")
	private boolean scoreUsed;
	
	@Column(name = "has_played")
	private boolean hasPlayed;
	
	@Column(name = "replacement_ind")
	private String replacementInd;
	
	public int getRound() {
		return round;
	}
	public void setRound(int round) {
		this.round = round;
	}
	public int getPlayerId() {
		return playerId;
	}
	public void setPlayerId(int playerId) {
		this.playerId = playerId;
	}
	public int getTeamPlayerId() {
		return teamPlayerId;
	}
	public void setTeamPlayerId(int teamPlayerId) {
		this.teamPlayerId = teamPlayerId;
	}
	public String getTeamCode() {
		return teamCode;
	}
	public void setTeamCode(String teamCode) {
		this.teamCode = teamCode;
	}
	public int isEmergency() {
		return isEmergency;
	}
	public void setEmergency(int isEmergency) {
		this.isEmergency = isEmergency;
	}
	public boolean isDnp() {
		return isDnp;
	}
	public void setDnp(boolean isDnp) {
		this.isDnp = isDnp;
	}
	public boolean isScoreUsed() {
		return scoreUsed;
	}
	public void setScoreUsed(boolean scoreUsed) {
		this.scoreUsed = scoreUsed;
	}
	public boolean hasPlayed() {
		return hasPlayed;
	}
	public void setHasPlayed(boolean hasPlayed) {
		this.hasPlayed = hasPlayed;
	}
	public String getReplacementInd() {
		return replacementInd;
	}
	public void setReplacementInd(String replacementInd) {
		this.replacementInd = replacementInd;
	}
	
	@Override
	public String toString() {
		return "DflSelectedPlayer [round=" + round + ", playerId=" + playerId + ", teamPlayerId=" + teamPlayerId
				+ ", teamCode=" + teamCode + ", isEmergency=" + isEmergency + ", isDnp=" + isDnp + ", scoreUsed="
				+ scoreUsed + ", hasPlayed=" + hasPlayed + ", replacementInd=" + replacementInd + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (hasPlayed ? 1231 : 1237);
		result = prime * result + (isDnp ? 1231 : 1237);
		result = prime * result + isEmergency;
		result = prime * result + playerId;
		result = prime * result + ((replacementInd == null) ? 0 : replacementInd.hashCode());
		result = prime * result + round;
		result = prime * result + (scoreUsed ? 1231 : 1237);
		result = prime * result + ((teamCode == null) ? 0 : teamCode.hashCode());
		result = prime * result + teamPlayerId;
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
		DflSelectedPlayer other = (DflSelectedPlayer) obj;
		if (hasPlayed != other.hasPlayed)
			return false;
		if (isDnp != other.isDnp)
			return false;
		if (isEmergency != other.isEmergency)
			return false;
		if (playerId != other.playerId)
			return false;
		if (replacementInd == null) {
			if (other.replacementInd != null)
				return false;
		} else if (!replacementInd.equals(other.replacementInd))
			return false;
		if (round != other.round)
			return false;
		if (scoreUsed != other.scoreUsed)
			return false;
		if (teamCode == null) {
			if (other.teamCode != null)
				return false;
		} else if (!teamCode.equals(other.teamCode))
			return false;
		if (teamPlayerId != other.teamPlayerId)
			return false;
		return true;
	}
}
