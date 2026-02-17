package org.sunbird.passbook.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "leaderboard_table", schema = "public")
public class LeaderboardEntity {

	@Id
	@Column(name = "userid", nullable = false)
	private String userId;

	@Column(name = "firstname")
	private String firstname;

	@Column(name = "lastname")
	private String lastname;

	@Column(name = "points")
	private Long points;

	@Column(name = "date")
	private LocalDateTime date;

	@Column(name = "professional_institute_name")
	private String professionalInstituteName;

	@Column(name = "rootorgid")
	private String rootOrgId;

	@Column(name = "district")
	private String district;

	@Column(name = "state")
	private String state;

	@Column(name = "profession")
	private String profession;

	@Column(name = "background")
	private String background;

	@Transient
	private Integer rank;
}