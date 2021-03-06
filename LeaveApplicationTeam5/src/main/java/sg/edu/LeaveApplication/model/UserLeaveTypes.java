package sg.edu.LeaveApplication.model;

import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

@Entity
public class UserLeaveTypes {
	@Id
	@GeneratedValue( strategy = GenerationType.AUTO)
	private Integer id;
	@ManyToOne
    private User user;
//	@ManyToMany
//    private Collection<LeaveTypes> leaveTypes;
	private int leaveAllowance;
	private String leaveName;
	
	public UserLeaveTypes() {
		super();
		// TODO Auto-generated constructor stub
	}
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
//	public Collection<LeaveTypes> getLeaveTypes() {
//		return leaveTypes;
//	}
//	public void setLeaveTypes(Collection<LeaveTypes> leaveTypes) {
//		this.leaveTypes = leaveTypes;
//	}
	public int getLeaveAllowance() {
		return leaveAllowance;
	}
	public void setLeaveAllowance(int leaveAllowance) {
		this.leaveAllowance = leaveAllowance;
	}

	public String getLeaveName() {
		return leaveName;
	}
	public void setLeaveName(String leaveName) {
		this.leaveName = leaveName;
	}
	@Override
	public String toString() {
		return "UserLeaveTypes [id=" + id + ",  leaveAllowance=" + leaveAllowance + ", leaveName=" + leaveName +"]";
	}
}
