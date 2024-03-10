package net.dflmngr.model.entity;

import java.util.Comparator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dfl_matthew_allen")
public class DflMatthewAllen implements Comparator<DflMatthewAllen>, Comparable<DflMatthewAllen> {
	
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id")
	private int id;
	
	private int round;
	private int game;
	
	@Column(name="player_id")
	private int playerId;

	private int score;
	private int votes;
	private int total;
	
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
	public int getGame() {
		return game;
	}
	public void setGame(int game) {
		this.game = game;
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
	public int getVotes() {
		return votes;
	}
	public void setVotes(int votes) {
		this.votes = votes;
	}
	public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
	}
	
	@Override
	public String toString() {
		return "DflMatthewAllen [id=" + id + ", round=" + round + ", game=" + game + ", playerId=" + playerId
				+ ", score=" + score + ", votes=" + votes + ", total=" + total + "]";
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + game;
		result = prime * result + id;
		result = prime * result + playerId;
		result = prime * result + round;
		result = prime * result + total;
		result = prime * result + votes;
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

		DflMatthewAllen other = (DflMatthewAllen) obj;
		return game == other.game
			&& id == other.id
			&& playerId == other.playerId
			&& round == other.round
			&& total == other.total
			&& votes == other.votes
			&& score == other.score;
	}

	@Override
	public int compareTo(DflMatthewAllen o) {
		if(this.getTotal() > o.getTotal()) {
			return 1;
		}
		return this.getTotal() < o.getTotal() ? -1 : 0;
	}
	
	@Override
	public int compare(DflMatthewAllen o1, DflMatthewAllen o2) {
		if(o1.getTotal() > o2.getTotal()) {
			return 1;
		}
		return o1.getTotal() < o2.getTotal() ? -1 : 0;
	}
}
