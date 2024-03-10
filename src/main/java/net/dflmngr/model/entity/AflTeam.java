package net.dflmngr.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "afl_team")
public class AflTeam {
			
	@Id
	@Column(name = "team_id")
	String teamId;
	
	String name;
	String nickname;
	String website;
	
	@Column(name = "senior_uri")
	String seniorUri;
	
	@Column(name = "rookie_uri")
	String rookieUri;

	@Column(name = "official_website")
	String officialWebsite;

	@Column(name = "official_senior_uri")
	String officialSeniorUri;
	
	@Column(name = "official_rookie_uri")
	String officialRookieUri;
	
	public String getTeamId() {
		return teamId;
	}
	public void setTeamId(String teamId) {
		this.teamId = teamId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getNickname() {
		return nickname;
	}
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
	public String getWebsite() {
		return website;
	}
	public void setWebsite(String website) {
		this.website = website;
	}
	public String getSeniorUri() {
		return seniorUri;
	}
	public void setSeniorUri(String seniorUri) {
		this.seniorUri = seniorUri;
	}
	public String getRookieUri() {
		return rookieUri;
	}
	public void setRookieUri(String rookieUri) {
		this.rookieUri = rookieUri;
	}
	public String getOfficialWebsite() {
		return officialWebsite;
	}
	public void setOfficialWebsite(String officialWebsite) {
		this.officialWebsite = officialWebsite;
	}
	public String getOfficialSeniorUri() {
		return officialSeniorUri;
	}
	public void setOffcialSeniorUri(String officialSeniorUri) {
		this.officialSeniorUri = officialSeniorUri;
	}
	public String getOfficialRookieUri() {
		return officialRookieUri;
	}
	public void setOfficialRookieUri(String officialRookieUri) {
		this.officialRookieUri = officialRookieUri;
	}

	@Override
	public String toString() {
		return "AflTeam [name=" + name + ", nickname=" + nickname + ", officialSeniorUri="
				+ officialSeniorUri + ", officialRookieUri=" + officialRookieUri + ", officialWebsite="
				+ officialWebsite + ", rookieUri=" + rookieUri + ", seniorUri=" + seniorUri
				+ ", teamId=" + teamId + ", website=" + website + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((nickname == null) ? 0 : nickname.hashCode());
		result = prime * result + ((officialSeniorUri == null) ? 0 : officialSeniorUri.hashCode());
		result = prime * result + ((officialRookieUri == null) ? 0 : officialRookieUri.hashCode());
		result = prime * result + ((officialWebsite == null) ? 0 : officialWebsite.hashCode());
		result = prime * result + ((rookieUri == null) ? 0 : rookieUri.hashCode());
		result = prime * result + ((seniorUri == null) ? 0 : seniorUri.hashCode());
		result = prime * result + ((teamId == null) ? 0 : teamId.hashCode());
		result = prime * result + ((website == null) ? 0 : website.hashCode());
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
		AflTeam other = (AflTeam) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (nickname == null) {
			if (other.nickname != null)
				return false;
		} else if (!nickname.equals(other.nickname))
			return false;
		if (officialSeniorUri == null) {
			if (other.officialSeniorUri != null)
				return false;
		} else if (!officialSeniorUri.equals(other.officialSeniorUri))
			return false;
		if (officialRookieUri == null) {
			if (other.officialRookieUri != null)
				return false;
		} else if (!officialRookieUri.equals(other.officialRookieUri))
			return false;
		if (officialWebsite == null) {
			if (other.officialWebsite != null)
				return false;
		} else if (!officialWebsite.equals(other.officialWebsite))
			return false;
		if (rookieUri == null) {
			if (other.rookieUri != null)
				return false;
		} else if (!rookieUri.equals(other.rookieUri))
			return false;
		if (seniorUri == null) {
			if (other.seniorUri != null)
				return false;
		} else if (!seniorUri.equals(other.seniorUri))
			return false;
		if (teamId == null) {
			if (other.teamId != null)
				return false;
		} else if (!teamId.equals(other.teamId))
			return false;
		if (website == null) {
			if (other.website != null)
				return false;
		} else if (!website.equals(other.website))
			return false;
		return true;
	}	
}
