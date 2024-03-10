package net.dflmngr.model.entity;

import java.time.ZonedDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "dfl_round_info")
public class DflRoundInfo {
	
	@Id
	@Column(name = "round")
	private int round;
	
	@Column(name = "hard_lockout")
	//@Temporal(TemporalType.TIMESTAMP)
	private ZonedDateTime hardLockoutTime;
	
	@Column(name = "split_round")
	private String splitRound;
	
	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name="round")
	private List<DflRoundEarlyGames> earlyGames;
	
	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name="round")
	private List<DflRoundMapping> roundMapping;
	
	
	public int getRound() {
		return round;
	}
	
	public void setRound(int round) {
		this.round = round;
	}
	
	public ZonedDateTime getHardLockoutTime() {
		return hardLockoutTime;
	}
	
	public void setHardLockoutTime(ZonedDateTime hardLockoutTime) {
		this.hardLockoutTime = hardLockoutTime;
	}
	
	public String getSplitRound() {
		return splitRound;
	}
	
	public void setSplitRound(String splitRound) {
		this.splitRound = splitRound;
	}
	
	public List<DflRoundEarlyGames> getEarlyGames() {
		return earlyGames;
	}

	public void setEarlyGames(List<DflRoundEarlyGames> earlyGames) {
		this.earlyGames = earlyGames;
	}

	public List<DflRoundMapping> getRoundMapping() {
		return roundMapping;
	}

	public void setRoundMapping(List<DflRoundMapping> roundMapping) {
		this.roundMapping = roundMapping;
	}

	@Override
	public String toString() {
		return "DflRoundInfo [round=" + round + ", hardLockoutTime=" + hardLockoutTime + ", splitRound=" + splitRound
				+ ", earlyGames=" + earlyGames + ", roundMapping=" + roundMapping + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((earlyGames == null) ? 0 : earlyGames.hashCode());
		result = prime * result + ((hardLockoutTime == null) ? 0 : hardLockoutTime.hashCode());
		result = prime * result + round;
		result = prime * result + ((roundMapping == null) ? 0 : roundMapping.hashCode());
		result = prime * result + ((splitRound == null) ? 0 : splitRound.hashCode());
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
		DflRoundInfo other = (DflRoundInfo) obj;
		if (earlyGames == null) {
			if (other.earlyGames != null)
				return false;
		} else if (!earlyGames.equals(other.earlyGames))
			return false;
		if (hardLockoutTime == null) {
			if (other.hardLockoutTime != null)
				return false;
		} else if (!hardLockoutTime.equals(other.hardLockoutTime))
			return false;
		if (round != other.round)
			return false;
		if (roundMapping == null) {
			if (other.roundMapping != null)
				return false;
		} else if (!roundMapping.equals(other.roundMapping))
			return false;
		if (splitRound == null) {
			if (other.splitRound != null)
				return false;
		} else if (!splitRound.equals(other.splitRound))
			return false;
		return true;
	}
}
