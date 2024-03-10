package net.dflmngr.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dfl_early_ins_and_outs")
public class DflEarlyInsAndOuts {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(columnDefinition = "serial")
	private Integer id;
	
	@Column(name = "team_code")
	private String teamCode;
	
	@Column(name = "round")
	private int round;
	
	@Column(name = "team_player_id")
	private int teamPlayerId;
	
	@Column(name = "in_or_out")
	private String inOrOut;
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}

	public String getTeamCode() {
		return teamCode;
	}
	
	public void setTeamCode(String teamCode) {
		this.teamCode = teamCode;
	}
	
	public int getRound() {
		return round;
	}
	
	public void setRound(int round) {
		this.round = round;
	}
	
	public int getTeamPlayerId() {
		return teamPlayerId;
	}
	
	public void setTeamPlayerId(int teamPlayerId) {
		this.teamPlayerId = teamPlayerId;
	}
	
	public String getInOrOut() {
		return inOrOut;
	}
	
	public void setInOrOut(String inOrOut) {
		this.inOrOut = inOrOut;
	}

	@Override
	public String toString() {
		return "DflEarlyInsAndOuts [id=" + id + ", teamCode=" + teamCode + ", round=" + round + ", teamPlayerId="
				+ teamPlayerId + ", inOrOut=" + inOrOut + "]";
	}

}
