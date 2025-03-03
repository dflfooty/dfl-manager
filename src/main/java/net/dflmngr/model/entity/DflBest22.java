package net.dflmngr.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="dfl_best_22")
public class DflBest22 {

	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id")
	private int id;
	
	private int round;

	@Column(name="player_id")
	private int playerId;
	
	int score;
	boolean bench;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
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
	public int getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score = score;
	}
	public boolean isBench() {
		return bench;
	}
	public void setBench(boolean bench) {
		this.bench = bench;
	}
	@Override
	public String toString() {
		return "DflBest22 [id=" + id + ", round=" + round + ", playerId=" + playerId + ", score=" + score + ", bench="
				+ bench + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (bench ? 1231 : 1237);
		result = prime * result + id;
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
		DflBest22 other = (DflBest22) obj;
		if (bench != other.bench)
			return false;
		if (id != other.id)
			return false;
		if (playerId != other.playerId)
			return false;
		if (round != other.round)
			return false;
		if (score != other.score)
			return false;
		return true;
	}
}
