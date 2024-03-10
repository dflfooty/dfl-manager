package net.dflmngr.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dfl_selection_ids")
public class DflSelectionIds {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(columnDefinition = "serial")
	private Integer id;

	@Column(name = "round")
	private int round;
	
	@Column(name = "team_code")
	private String teamCode;

	@Column(name = "selection_id")
	private String selectionId;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public int getRound() {
		return round;
	}

	public void setRound(int round) {
		this.round = round;
	}

	public String getTeamCode() {
		return teamCode;
	}

	public void setTeamCode(String teamCode) {
		this.teamCode = teamCode;
	}

	public String getSelectionId() {
		return selectionId;
	}

	public void setSelectionId(String selectionId) {
		this.selectionId = selectionId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + round;
		result = prime * result + ((selectionId == null) ? 0 : selectionId.hashCode());
		result = prime * result + ((teamCode == null) ? 0 : teamCode.hashCode());
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
		DflSelectionIds other = (DflSelectionIds) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (round != other.round)
			return false;
		if (selectionId == null) {
			if (other.selectionId != null)
				return false;
		} else if (!selectionId.equals(other.selectionId))
			return false;
		if (teamCode == null) {
			if (other.teamCode != null)
				return false;
		} else if (!teamCode.equals(other.teamCode))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DlfSelectionIds [id=" + id + ", round=" + round + ", teamCode=" + teamCode + ", selectionId="
				+ selectionId + "]";
	}
}
