package sg.edu.LeaveApplication.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

import sg.edu.LeaveApplication.model.LeaveRecord;
import sg.edu.LeaveApplication.model.LeaveTypes;
import sg.edu.LeaveApplication.model.PublicHolidays;
import sg.edu.LeaveApplication.model.Status;
import sg.edu.LeaveApplication.model.User;
import sg.edu.LeaveApplication.service.LeaveService;
import sg.edu.LeaveApplication.service.LeaveTypeService;
import sg.edu.LeaveApplication.service.PublicHolidayService;
import sg.edu.LeaveApplication.service.UserLeaveTypesService;
import sg.edu.LeaveApplication.service.UserService;

@Controller
@SessionAttributes("user")
@RequestMapping("/leave")
public class LeaveController {
	@Autowired
	LeaveService leaveservice;

	@Autowired
	public void setLeaveservice(LeaveService leaveservice) {
		this.leaveservice = leaveservice;
	}

	@Autowired
	LeaveTypeService leavetypeservice;

	@Autowired
	public void setLeavetypeservice(LeaveTypeService leavetypeservice) {
		this.leavetypeservice = leavetypeservice;
	}

	@Autowired
	UserService uservice;

	@Autowired
	public void setUservice(UserService uservice) {
		this.uservice = uservice;
	}

	@Autowired
	PublicHolidayService holiservice;

	@Autowired
	public void setHoliservice(PublicHolidayService holiservice) {
		this.holiservice = holiservice;
	}

	@Autowired
	UserLeaveTypesService ultservice;

	@Autowired
	public void setUltservice(UserLeaveTypesService ultservice) {
		this.ultservice = ultservice;
	}

	// return leaveDayCost upon cancel/delete/reject
	public void returnLeave(LeaveRecord lr) {
		int currentBalance = ultservice.findleaveAllowance(lr.getUser().getId(), lr.getLeaveTypes().getLeaveName());
		lr.setLeaveDayCost(currentBalance + lr.getLeaveDayCost());
		leaveservice.saveLeave(lr);
	}

	// check balance function
	public Boolean isBalanceEnough(User user, String leaveName, Integer leaveCost) {

		if (ultservice.findleaveAllowance(user.getId(), leaveName) >= leaveCost) {
			return true;
		}
		return false;
	}

	// to move to leave service after done
	public static Integer getLeaveDayCost(String sd, String ed, List<LocalDate> holidays) {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate date1 = LocalDate.parse(sd, formatter);
		LocalDate date2 = LocalDate.parse(ed, formatter);

		Predicate<LocalDate> isWeekend = date -> date.getDayOfWeek() == DayOfWeek.SATURDAY
				&& date.getDayOfWeek() == DayOfWeek.SUNDAY;

		Predicate<LocalDate> isHoliday = date -> holidays.contains(date);
		long daysBetween = ChronoUnit.DAYS.between(date1, date2) + 1;

		long leaveCost = Stream.iterate(date1, date -> date.plusDays(1)).limit(daysBetween)
				.filter(isHoliday.or(isWeekend).negate()).count();
		return (int) leaveCost;
	}

	@RequestMapping("/emp/leave/list")
	public String list(Model model) {
		model.addAttribute("leaveList", leaveservice.findAll());
		return "leaveList";
	}

	@RequestMapping("/emp/leave/apply")
	public String applyLeave(Model model) {
		// replace once user session is ready
		User sessionUser = uservice.findUserById(21);

		model.addAttribute("leave", new LeaveRecord());
		model.addAttribute("leaveTypes", leavetypeservice.findAll());
		model.addAttribute("phlist", holiservice.findAll());
		model.addAttribute("balanceList", ultservice.findAllByUser(sessionUser));
		return "createLeave";
	}

	@RequestMapping("/emp/save")
	public String saveLeave(@ModelAttribute("leave") @Valid LeaveRecord leaverecord, BindingResult result, Model model,
			@RequestParam("startDate") String sd, @RequestParam("endDate") String ed) throws ParseException {

		// replace when session is ready
		User sessionUser = uservice.findUserById(21);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		// calculate duration
		Date date1 = new SimpleDateFormat("yyyy-MM-dd").parse(sd);
		Date date2 = new SimpleDateFormat("yyyy-MM-dd").parse(ed);
		long time = date2.getTime() - date1.getTime();
		Integer duration = (int) time / (1000 * 3600 * 24) + 1;

		// check whether need to deduct PH and weekend
		Integer balance = ultservice.findleaveAllowance(sessionUser.getId(),
				leaverecord.getLeaveTypes().getLeaveName());

		if (duration <= 14) {
			// calculate leave cost = duration - weekend - PH
			List<PublicHolidays> holidays = holiservice.findAll();
			List<LocalDate> allPHdays = holidays.stream().map(PublicHolidays::getDate).collect(Collectors.toList());
			Integer leaveCost = getLeaveDayCost(sd, ed, allPHdays);
			// validate balance
			if (isBalanceEnough(sessionUser, leaverecord.getLeaveTypes().getLeaveName(), leaveCost)) {
				// set balance - leaveCost
				ultservice.update(sessionUser, leaverecord.getLeaveTypes().getLeaveName(), balance - leaveCost);
				leaverecord.setLeaveDayCost(leaveCost);
			} else {
				model.addAttribute("msg", "You do not have enough balance.");
				return "redirect:/emp/apply";
			}
		} else {

			// validate balance
			if (isBalanceEnough(sessionUser, leaverecord.getLeaveTypes().getLeaveName(), duration)) {
				// balance - duration
				ultservice.update(sessionUser, leaverecord.getLeaveTypes().getLeaveName(), balance - duration);
				leaverecord.setLeaveDayCost(duration);
			} else {
				model.addAttribute("msg", "You do not have enough balance.");
				return "redirect:/emp/apply";
			}
		}

		leaverecord.setLeaveTypes(leaverecord.getLeaveTypes());
		leaverecord.setUser(uservice.findUserById(102));// to use session user_id
		leaverecord.setLeaveAppliedDate(new Date());
		leaverecord.setDuration(duration);
		leaverecord.setStartDate(LocalDate.parse(sd, formatter));

		// if new record, set to Pending; otherwise as Updated
		if (leaverecord.getStatus() == null) {

			leaverecord.setStatus(Status.PENDING);
		} else {
			leaverecord.setStatus(Status.UPDATED);
		}

		leaveservice.saveLeave(leaverecord);
		return "redirect:/emp/list";
	}

	@RequestMapping("/emp/update/{id}")
	public String updateLeave(@PathVariable("id") Integer id, Model model) {

		model.addAttribute("leaveTypes", leavetypeservice.findAll());
		model.addAttribute("phlist", holiservice.findAll());
		LeaveRecord lr = leaveservice.findLeaveRecordById(id);
		// only when Pending, allow update
		if (lr != null && lr.getStatus() == Status.PENDING || lr.getStatus() == Status.UPDATED) {
			lr.setStatus(Status.UPDATED);
			model.addAttribute("leave", lr);
			return "createLeave";
		}
		return "redirect:/leave/list";
	}

	@RequestMapping("/emp/detail/{id}")
	public String viewLeave(@PathVariable("id") Integer id, Model model) {
		model.addAttribute("leave", leaveservice.findLeaveRecordById(id));
		return "leaveDetails";
	}

	@RequestMapping("/emp/delete/{id}")
	public String deleteLeave(@PathVariable("id") Integer id, Model model) {
		LeaveRecord lr = leaveservice.findLeaveRecordById(id);
		// only when Pending, allow delete
		if (lr != null && lr.getStatus() == Status.PENDING || lr.getStatus() == Status.UPDATED) {
			returnLeave(lr);
			leaveservice.deleteLeave(lr);
			model.addAttribute("msg", "Leave is deleted. ");
		} else {
			model.addAttribute("msg", "Cannot delete leave after approved or cancelled.");
		}
		return "forward:/leave/list";
	}

	@RequestMapping("/emp/cancel/{id}")
	public String cancelLeave(@PathVariable("id") Integer id, Model model) {
		LeaveRecord lr = leaveservice.findLeaveRecordById(id);
		// have record and after approved, allow cancel
		if (lr != null && lr.getStatus() == Status.APPROVED) {
			leaveservice.cancelLeave(lr);
			returnLeave(lr);
			model.addAttribute("msg", "Leave is cancelled.");
		} else {
			model.addAttribute("msg", "Leave can only be canceld after approved.");
		}
		return "forward:/leave/list";
	}

	@RequestMapping("viewLeave")
	public String viewLeaveRequest(Model model, String keyword, String ltName) {
		model.addAttribute("ltNames", leavetypeservice.findAllLeaveTypeNames());
		if (keyword != null || ltName != null) {
			model.addAttribute("leaveList", leaveservice.findLeaveByEmployeeAndLeave(keyword, ltName));
			System.out.println(ltName);
		} else {
			model.addAttribute("leaveList", leaveservice.findAllPendingLeave());
			System.out.println(ltName);
		}
		return "/manager/viewLeaveRequests";
	}

	@RequestMapping("/viewLeaveHistory")
	public String viewLeaveHistory(Model model, String keyword, String fromDate, String toDate, String ltName,
			String status) {
		model.addAttribute("ltNames", leavetypeservice.findAllLeaveTypeNames());
		// System.out.println("k"+keyword);System.out.println("Sd"+startDate);System.out.println("ed"+endDate);System.out.println("na"+ltName);System.out.println("status"+status);
		if ((keyword != ""&& keyword != null) ||  // Search Part
			(ltName != ""&& ltName != null)  ||
			(fromDate != ""&& fromDate != null) || 
			(toDate != ""&& toDate != null) ||
			(status != ""&& status != null)) {
			
			
			Integer int_status = -1;
			if (status == "Pending")
				 int_status = Status.PENDING.ordinal(); 
			if (status == "Approved")
				int_status = Status.APPROVED.ordinal();;
			if (status == "Rejected")
				int_status = Status.REJECTED.ordinal();
			if (status == "Updated")
				int_status = Status.UPDATED.ordinal();
			if (status == "Cancelled")
				int_status = Status.CANCELLED.ordinal();
			if (status == "Deleted")
				int_status = Status.DELETED.ordinal();

			if ((fromDate != ""&& fromDate != null) || 
					(toDate != ""&& toDate != null) )// Search with date
			{

				LocalDate startDate = LocalDate.parse(fromDate);
				LocalDate endDate = LocalDate.parse(toDate);
				model.addAttribute("leaveHistoryList",
						leaveservice.findLeaveHistoryByDate(keyword, startDate, endDate, ltName, int_status));
				System.out.println(ltName);
			} else // Search Without date
			{
				model.addAttribute("leaveHistoryList", leaveservice.findLeaveHistory(keyword, ltName, int_status));

			}

		} else {
			model.addAttribute("leaveHistoryList", leaveservice.findAll());
			System.out.println(ltName);
		}
		return "/manager/viewLeaveHistory";
	}

	@RequestMapping("/details/{id}")
	public String showLeaveDetails(@PathVariable("id") Integer id, Model model) {
		model.addAttribute("leave", leaveservice.findLeaveRecordById(id));
		/* model.addAttribute("leaveStatus", leaveservice.findAllLeaveStatusName()); */
		return "/manager/pendingLeaveDetails";
	}

	@RequestMapping("/submit/{id}")
	public String submit(@ModelAttribute("leave") LeaveRecord leave, @PathVariable("id") Integer id,
			@RequestParam("comments") String comments, @RequestParam("status") Status status) {
		LeaveRecord newLeave = leaveservice.findLeaveRecordById(leave.getId());
		newLeave.setComments(comments);
		newLeave.setStatus(status);
		if (status.equals(status.REJECTED)) {
			int addback = newLeave.getLeaveDayCost();
			int userId = newLeave.getUser().getId();
			String leaveName = newLeave.getLeaveTypes().getLeaveName();
			int leaveAllowance = ultservice.findleaveAllowance(userId, leaveName);
			int newAllowance = leaveAllowance + addback;
			ultservice.update(newLeave.getUser(), leaveName, newAllowance);
		}
		leaveservice.saveLeave(newLeave);
		SendEamilController.SendEmailSSL(status.name(),
				newLeave.getUser().getFirstName() + " " + newLeave.getUser().getLastName(), comments,
				newLeave.getUser().getEmail());
		return "redirect:/leave/viewLeave";
	}

}
