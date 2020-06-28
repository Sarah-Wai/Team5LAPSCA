package sg.edu.LeaveApplication.model;

import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotEmpty;

@Entity
public class UserLeaveTypes {
	@Id
	@GeneratedValue( strategy = GenerationType.AUTO)
	private int id;
	@ManyToOne
    private User user;
	@ManyToMany
    private Collection<LeaveTypes> leaveTypes;
	@NotEmpty
	private int leaveAllowance;
	public UserLeaveTypes() {
		super();
		// TODO Auto-generated constructor stub
	}
	public UserLeaveTypes(User user, Collection<LeaveTypes> leaveTypes, @NotEmpty int leaveAllowance) {
		super();
		this.user = user;
		this.leaveTypes = leaveTypes;
		this.leaveAllowance = leaveAllowance;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	public Collection<LeaveTypes> getLeaveTypes() {
		return leaveTypes;
	}
	public void setLeaveTypes(Collection<LeaveTypes> leaveTypes) {
		this.leaveTypes = leaveTypes;
	}
	public int getLeaveAllowance() {
		return leaveAllowance;
	}
	public void setLeaveAllowance(int leaveAllowance) {
		this.leaveAllowance = leaveAllowance;
	}
	@Override
	public String toString() {
		return "UserLeaveTypes [id=" + id + ", leaveTypes=" + leaveTypes + ", leaveAllowance=" + leaveAllowance + "]";
	}
	
}
