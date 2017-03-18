package net.dflmngr.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="dfl_preseason_scores")
public class DflPreseasonScores {
	
	@Id
	@Column(name="player_id")
	private int playerId;
	
	@Id
	private int round;
	
	@Id
	private int score;

	public int getPlayerId() {
		return playerId;
	}

	public void setPlayerId(int playerId) {
		this.playerId = playerId;
	}

	public int getRound() {
		return round;
	}

	public void setRound(int round) {
		this.round = round;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	@Override
	public String toString() {
		return "DflPreseasonScores [playerId=" + playerId + ", round=" + round + ", score=" + score + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + playerId;
		result = prime * result + round;
		result = prime * result + score;
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
		DflPreseasonScores other = (DflPreseasonScores) obj;
		if (playerId != other.playerId)
			return false;
		if (round != other.round)
			return false;
		if (score != other.score)
			return false;
		return true;
	}
}
