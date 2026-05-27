package com.git.recruitment.service;

import com.git.recruitment.dto.DTOs.*;
import com.git.recruitment.event.JobCreatedEvent;
import com.git.recruitment.event.UserActionEvent;
import com.git.recruitment.event.UserActionType;
import com.git.recruitment.model.*;
import com.git.recruitment.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class JobService {
    private static final String ROUND_LOCATION_PATTERN = "^[A-Za-z0-9 .,#:/()\\-]+$";
    private static final String ROUND_DESCRIPTION_PATTERN = "^[A-Za-z0-9 .,;:()'\"/&+\\-]+$";

    private final JobRepository jobRepository;
    private final JobApplicationRepository appRepository;
    private final InterviewRoundRepository roundRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;

    // ── Admin: Create Job ─────────────────────────────────────
    @Transactional
    public JobResponse createJob(JobRequest req) {
        validateJobRequiredSkills(req.getSkills());
        Job job = Job.builder()
                .title(req.getTitle()).companyName(req.getCompanyName())
                .description(req.getDescription()).location(req.getLocation())
                .jobType(req.getJobType()).salaryPackage(req.getSalaryPackage())
                .eligibleBranches(req.getEligibleBranches()).eligibleBatches(req.getEligibleBatches()).minimumCgpa(req.getMinimumCgpa())
                .skills(req.getSkills()).applicationDeadline(req.getApplicationDeadline())
                .totalPositions(req.getTotalPositions())
                .numberOfRounds(req.getNumberOfRounds() != null ? req.getNumberOfRounds() : 2)
                .status(req.getStatus() != null ? Job.JobStatus.valueOf(req.getStatus()) : Job.JobStatus.ACTIVE)
                .build();
        Job saved = jobRepository.save(job);
        eventPublisher.publishEvent(new JobCreatedEvent(saved));
        log.info("Published job created event. jobId={}", saved.getId());
        return toJobResponse(saved, null);
    }

    // ── Admin: Update Job ─────────────────────────────────────
    @Transactional
    public JobResponse updateJob(Long id, JobRequest req) {
        if (req.getSkills() != null) validateJobRequiredSkills(req.getSkills());
        Job job = getJob(id);
        if (req.getTitle()              != null) job.setTitle(req.getTitle());
        if (req.getCompanyName()        != null) job.setCompanyName(req.getCompanyName());
        if (req.getDescription()        != null) job.setDescription(req.getDescription());
        if (req.getLocation()           != null) job.setLocation(req.getLocation());
        if (req.getJobType()            != null) job.setJobType(req.getJobType());
        if (req.getSalaryPackage()      != null) job.setSalaryPackage(req.getSalaryPackage());
        if (req.getEligibleBranches()   != null) job.setEligibleBranches(req.getEligibleBranches());
        if (req.getEligibleBatches()    != null) job.setEligibleBatches(req.getEligibleBatches());
        if (req.getMinimumCgpa()        != null) job.setMinimumCgpa(req.getMinimumCgpa());
        if (req.getSkills()             != null) job.setSkills(req.getSkills());
        if (req.getApplicationDeadline()!= null) job.setApplicationDeadline(req.getApplicationDeadline());
        if (req.getTotalPositions()     != null) job.setTotalPositions(req.getTotalPositions());
        if (req.getNumberOfRounds()     != null) job.setNumberOfRounds(req.getNumberOfRounds());
        if (req.getStatus()             != null) job.setStatus(Job.JobStatus.valueOf(req.getStatus()));
        return toJobResponse(jobRepository.save(job), null);
    }

    // ── Admin: Delete Job ─────────────────────────────────────
    @Transactional
    public void deleteJob(Long id) {
        Job job = getJob(id);
        List<JobApplication> applications = appRepository.findByJob(job);

        applications.forEach(application ->
                roundRepository.deleteAll(roundRepository.findByApplication(application)));
        appRepository.deleteAll(applications);
        jobRepository.delete(job);
    }

    // ── Admin: Get All Jobs ───────────────────────────────────
    public List<JobResponse> getAllJobsAdmin() {
        return jobRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(j -> toJobResponse(j, null)).collect(Collectors.toList());
    }

    // ── Student: Get Active Jobs ──────────────────────────────
    public List<JobResponse> getActiveJobs(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        return jobRepository.findByStatusOrderByCreatedAtDesc(Job.JobStatus.ACTIVE).stream()
                .map(j -> {
                    JobApplication myApplication = user != null
                            ? appRepository.findByUserAndJob(user, j).orElse(null)
                            : null;
                    return toJobResponse(j, myApplication);
                }).collect(Collectors.toList());
    }

    // ── Student: Apply for Job ────────────────────────────────
    @Transactional
    public ApplicationResponse applyForJob(String email, Long jobId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Job job = getJob(jobId);

        if (appRepository.existsByUserAndJob(user, job))
            throw new IllegalStateException("You have already applied for this job.");
        if (getEffectiveJobStatus(job) != Job.JobStatus.ACTIVE)
            throw new IllegalStateException("This job is no longer accepting applications.");
        if (!isBatchEligible(job, user))
            throw new IllegalStateException("This job is not available for your batch year.");
        if (!isCgpaEligible(job, user))
            throw new IllegalStateException("Your CGPA is below the minimum required CGPA for this job.");

        JobApplication app = JobApplication.builder()
                .user(user).job(job).status(JobApplication.AppStatus.APPLIED).build();
        appRepository.save(app);
        emailService.sendJobApplicationConfirmation(user, job);
        return toAppResponse(app);
    }

    // ── Student: My Applications ──────────────────────────────
    @Transactional
    public List<ApplicationResponse> getMyApplications(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return appRepository.findByUserOrderByAppliedAtDesc(user).stream()
                .map(this::toAppResponse).collect(Collectors.toList());
    }

    // ── Admin: Get Applicants for a Job ──────────────────────
        @Transactional
        public List<ApplicationResponse> getApplicantsByJob(Long jobId) {
        Job job = getJob(jobId);
        return appRepository.findByJobOrderByAppliedAtDesc(job).stream()
                .map(this::toAppResponseAdmin).collect(Collectors.toList());
    }

    // ── Admin: Update Application Status ─────────────────────
    @Transactional
    public ApplicationResponse updateApplicationStatus(Long appId, String status) {
        JobApplication app = getApp(appId);
        app.setStatus(JobApplication.AppStatus.valueOf(status));
        JobApplication saved = appRepository.save(app);
        publishApplicationAction(saved, UserActionType.PROFILE_UPDATED,
                "Your application status for " + jobLabel(saved.getJob()) + " was updated to " + saved.getStatus().name() + ".");
        return toAppResponse(saved);
    }

    // ── Admin: Schedule Aptitude Test ────────────────────────
    @Transactional
    public ApplicationResponse scheduleAptitude(Long appId) {
        JobApplication app = getApp(appId);
        app.setStatus(JobApplication.AppStatus.APTITUDE_SCHEDULED);
        JobApplication saved = appRepository.save(app);
        publishApplicationAction(saved, UserActionType.APTITUDE_SCHEDULED,
                "An aptitude round has been scheduled for your application to " + jobLabel(saved.getJob()) + ".");
        return toAppResponse(saved);
    }

    // ── Admin: Record Aptitude Result ────────────────────────
    @Transactional
    public ApplicationResponse recordAptitudeResult(Long appId, AptitudeResultRequest req) {
        JobApplication app = getApp(appId);
        app.setAptitudeCleared(req.getCleared());
        app.setAptitudeScore(req.getScore());
        app.setStatus(req.getCleared()
                ? JobApplication.AppStatus.APTITUDE_CLEARED
                : JobApplication.AppStatus.APTITUDE_FAILED);
        JobApplication saved = appRepository.save(app);
        publishApplicationAction(saved, UserActionType.APTITUDE_RESULT_RECORDED,
                "Your aptitude result for " + jobLabel(saved.getJob()) + " has been recorded as "
                        + (Boolean.TRUE.equals(req.getCleared()) ? "cleared" : "not cleared")
                        + (req.getScore() != null ? " with score " + req.getScore() + "." : "."));
        return toAppResponse(saved);
    }

    // ── Admin: Schedule Interview ─────────────────────────────
    @Transactional
    public ApplicationResponse scheduleInterview(Long appId, InterviewScheduleRequest req) {
        JobApplication app = getApp(appId);
        app.setInterviewDate(req.getInterviewDate());
        app.setInterviewTime(req.getInterviewTime());
        app.setInterviewMode(req.getInterviewMode());
        app.setStatus(JobApplication.AppStatus.INTERVIEW_SCHEDULED);
        appRepository.save(app);
        emailService.sendInterviewScheduled(app.getUser(), app.getJob(), app);
        return toAppResponse(app);
    }

    // ── Admin: Record Interview Result ────────────────────────
    @Transactional
    public ApplicationResponse recordInterviewResult(Long appId, InterviewResultRequest req) {
        JobApplication app = getApp(appId);
        app.setInterviewResult(req.getResult());
        app.setInterviewNotes(req.getNotes());
        app.setFinalSelected(req.getFinalSelected());
        if (Boolean.TRUE.equals(req.getFinalSelected())) {
            app.setStatus(JobApplication.AppStatus.SELECTED);
        } else {
            app.setStatus(JobApplication.AppStatus.REJECTED);
            publishApplicationAction(app, UserActionType.FINAL_DECISION_RECORDED,
                    "The final decision for your application to " + jobLabel(app.getJob()) + " has been recorded as not selected.");
        }
        JobApplication saved = appRepository.save(app);
        // Send email AFTER save so a mail failure can never roll back the status change
        if (Boolean.TRUE.equals(req.getFinalSelected())) {
            emailService.sendFinalSelection(saved.getUser(), saved.getJob());
        }
        return toAppResponse(saved);
    }

    // ── Helpers ───────────────────────────────────────────────
    @Transactional
    public ApplicationResponse rejectApplication(Long appId) {
        JobApplication app = getApp(appId);
        app.setFinalSelected(false);
        // Only set interview-level fields if the application was actually in the interview process
        boolean wasInInterview = app.getStatus() == JobApplication.AppStatus.INTERVIEW_SCHEDULED
                || app.getStatus() == JobApplication.AppStatus.APTITUDE_CLEARED
                || app.getStatus() == JobApplication.AppStatus.APTITUDE_FAILED;
        if (wasInInterview || app.getInterviewResult() == null) {
            app.setInterviewResult("Rejected");
        }
        if (app.getInterviewNotes() == null || app.getInterviewNotes().isBlank()) {
            app.setInterviewNotes("Rejected by admin before completing the selection process.");
        }
        app.setStatus(JobApplication.AppStatus.REJECTED);
        JobApplication saved = appRepository.save(app);
        publishApplicationAction(saved, UserActionType.APPLICATION_REJECTED,
                "Your application to " + jobLabel(saved.getJob()) + " has been rejected by the Placement Office.");
        return toAppResponse(saved);
    }

    private Job getJob(Long id) {
        return jobRepository.findById(id).orElseThrow(() -> new RuntimeException("Job not found: " + id));
    }
    private JobApplication getApp(Long id) {
        return appRepository.findById(id).orElseThrow(() -> new RuntimeException("Application not found: " + id));
    }

    private JobResponse toJobResponse(Job j, JobApplication myApplication) {
        long count = appRepository.countByJob(j);
        return JobResponse.builder()
                .id(j.getId()).title(j.getTitle()).companyName(j.getCompanyName())
                .description(j.getDescription()).location(j.getLocation()).jobType(j.getJobType())
                .salaryPackage(j.getSalaryPackage()).eligibleBranches(j.getEligibleBranches()).eligibleBatches(j.getEligibleBatches())
                .minimumCgpa(j.getMinimumCgpa()).skills(j.getSkills())
                .applicationDeadline(j.getApplicationDeadline()).totalPositions(j.getTotalPositions())
                .numberOfRounds(j.getNumberOfRounds())
                .status(getEffectiveJobStatus(j).name()).createdAt(j.getCreatedAt())
                .applicantCount(count)
                .alreadyApplied(myApplication != null)
                .myApplicationStatus(myApplication != null ? myApplication.getStatus().name() : null)
                .myCurrentRoundDisplayStatus(buildCurrentRoundDisplayStatus(myApplication))
                .build();
    }

    private String buildCurrentRoundDisplayStatus(JobApplication application) {
        if (application == null
                || application.getStatus() == JobApplication.AppStatus.SELECTED
                || application.getStatus() == JobApplication.AppStatus.REJECTED) {
            return null;
        }

        return roundRepository.findByApplicationOrderByRoundNumberAsc(application).stream()
                .reduce((first, second) -> second)
                .map(round -> {
                    String roundName = round.getRoundType() != null && !round.getRoundType().isBlank()
                            ? round.getRoundType()
                            : "Round " + round.getRoundNumber();
                    return roundName + " " + getRoundDisplayState(round);
                })
                .orElse(null);
    }

    private boolean isBatchEligible(Job job, User user) {
        if (user == null) return true;

        List<String> eligibleBatches = splitCsv(job.getEligibleBatches());
        if (eligibleBatches.isEmpty()) return true;

        String studentBatchYear = user.getBatchYear() != null ? user.getBatchYear().trim() : "";
        if (studentBatchYear.isEmpty()) return false;

        return eligibleBatches.contains(studentBatchYear);
    }

    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) return List.of();

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.toList());
    }

    private Job.JobStatus getEffectiveJobStatus(Job job) {
        if (job.getStatus() == Job.JobStatus.CLOSED) {
            return Job.JobStatus.CLOSED;
        }

        if (job.getApplicationDeadline() != null && job.getApplicationDeadline().isBefore(LocalDate.now())) {
            return Job.JobStatus.CLOSED;
        }

        return job.getStatus();
    }

    private ApplicationResponse toAppResponse(JobApplication a) {
        ApplicationResponse response = ApplicationResponse.builder()
                .id(a.getId()).jobId(a.getJob().getId())
                .jobTitle(a.getJob().getTitle()).companyName(a.getJob().getCompanyName())
                .status(a.getStatus().name())
                .aptitudeCleared(a.getAptitudeCleared()).aptitudeScore(a.getAptitudeScore())
                .interviewDate(a.getInterviewDate()).interviewTime(a.getInterviewTime())
                .interviewMode(a.getInterviewMode()).interviewResult(a.getInterviewResult())
                .interviewNotes(a.getInterviewNotes()).finalSelected(a.getFinalSelected())
                .appliedAt(a.getAppliedAt()).build();

        enrichCurrentRoundDetails(response, a);
        return response;
    }

    private ApplicationResponse toAppResponseAdmin(JobApplication a) {
        ApplicationResponse r = toAppResponse(a);
        r.setUserId(a.getUser().getId());
        r.setStudentName(a.getUser().getFirstName() + " " + a.getUser().getLastName());
        r.setStudentEmail(a.getUser().getEmail());
        r.setRollNumber(a.getUser().getRollNumber());
        r.setDepartment(a.getUser().getDepartment());
        r.setCgpa(a.getUser().getCgpa());
        return r;
    }

    // ── Round Scheduling ─────────────────────────────────────
    @Transactional
    public RoundSchedulingView getRoundSchedulingView(Long appId) {
        JobApplication app = appRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        
        List<InterviewRound> rounds = roundRepository.findByApplicationOrderByRoundNumberAsc(app);
        List<InterviewRoundResponse> roundResponses = rounds.stream()
                .map(this::toRoundResponse).collect(Collectors.toList());
        
        return RoundSchedulingView.builder()
                .applicationId(app.getId()).jobId(app.getJob().getId())
                .studentId(app.getUser().getId())
                .studentName(app.getUser().getFirstName() + " " + app.getUser().getLastName())
                .studentEmail(app.getUser().getEmail()).rollNumber(app.getUser().getRollNumber())
                .jobTitle(app.getJob().getTitle()).companyName(app.getJob().getCompanyName())
                .numberOfRounds(getConfiguredRoundCount(app.getJob()))
                .currentRound(app.getCurrentRound() != null ? app.getCurrentRound() : 0)
                .jobApplicationStatus(app.getStatus().name())
                .rounds(roundResponses)
                .build();
    }

    @Transactional
    public InterviewRoundResponse scheduleRound(Long appId, InterviewRoundRequest req) {
        JobApplication app = appRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        List<InterviewRound> existingRounds = roundRepository.findByApplicationOrderByRoundNumberAsc(app);
        if (app.getStatus() == JobApplication.AppStatus.SELECTED || app.getStatus() == JobApplication.AppStatus.REJECTED) {
            throw new RuntimeException("This application is already closed.");
        }
        if (req.getRoundNumber() == null || req.getRoundNumber() < 1) {
            throw new RuntimeException("Please choose a valid round number.");
        }

        validateRoundScheduleRequest(req);
        LocalDate scheduledDate = LocalDate.parse(req.getScheduledDate().trim());
        Optional<InterviewRound> existingRound = roundRepository.findByApplicationAndRoundNumber(app, req.getRoundNumber());

        if (existingRound.isPresent()) {
            InterviewRound round = existingRound.get();
            if (!Boolean.TRUE.equals(req.getRescheduleExisting())) {
                throw new RuntimeException("You already scheduled this round. Click OK to reschedule it, or Cancel to keep the current schedule.");
            }
            if (round.getStatus() == InterviewRound.RoundStatus.COMPLETED) {
                throw new RuntimeException("This round is already completed. Please schedule the next round instead.");
            }

            round.setRoundType(req.getRoundType().trim());
            round.setScheduledDate(scheduledDate);
            round.setScheduledTime(req.getScheduledTime().trim());
            round.setInterviewMode(req.getInterviewMode().trim());
            round.setLocation(req.getLocation().trim());
            round.setDescription(req.getDescription().trim());
            round.setStatus(InterviewRound.RoundStatus.SCHEDULED);
            round.setResult("PENDING");
            round.setScore(null);
            round.setNotes(null);
            round.setFeedback(null);

            InterviewRound saved = roundRepository.save(round);
            app.setCurrentRound(req.getRoundNumber());
            app.setStatus(getScheduledStatusForRoundType(req.getRoundType()));
            appRepository.save(app);
            publishRoundScheduled(app, saved, true);
            return toRoundResponse(saved);
        }

        int expectedRoundNumber = existingRounds.size() + 1;
        if (req.getRoundNumber() != expectedRoundNumber) {
            throw new RuntimeException("Please schedule round " + expectedRoundNumber + " next.");
        }
        boolean hasPendingRound = existingRounds.stream()
                .anyMatch(round -> round.getStatus() == InterviewRound.RoundStatus.SCHEDULED);
        if (hasPendingRound) {
            throw new RuntimeException("Complete the currently scheduled round before adding another round.");
        }

        InterviewRound round = InterviewRound.builder()
                .application(app).roundNumber(req.getRoundNumber())
                .roundType(req.getRoundType().trim()).scheduledDate(scheduledDate)
                .scheduledTime(req.getScheduledTime().trim()).interviewMode(req.getInterviewMode().trim())
                .location(req.getLocation().trim()).description(req.getDescription().trim())
                .status(InterviewRound.RoundStatus.SCHEDULED).result("PENDING")
                .build();

        InterviewRound saved = roundRepository.save(round);
        app.setCurrentRound(req.getRoundNumber());
        app.setStatus(getScheduledStatusForRoundType(req.getRoundType()));
        appRepository.save(app);
        publishRoundScheduled(app, saved, false);

        return toRoundResponse(saved);
    }

    private int getConfiguredRoundCount(Job job) {
        return job.getNumberOfRounds() != null && job.getNumberOfRounds() > 0
                ? job.getNumberOfRounds()
                : 2;
    }

    private boolean isCgpaEligible(Job job, User user) {
        if (job.getMinimumCgpa() == null) return true;
        if (user.getCgpa() == null || user.getCgpa().isBlank()) return false;
        try {
            return Double.parseDouble(user.getCgpa().trim()) >= job.getMinimumCgpa();
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void validateJobRequiredSkills(String skills) {
        if (skills == null || skills.isBlank()) return;
        boolean hasNumberOnlySkill = Arrays.stream(skills.split(","))
                .map(String::trim)
                .filter(skill -> !skill.isBlank())
                .anyMatch(skill -> !skill.matches(".*[A-Za-z].*"));
        if (hasNumberOnlySkill) {
            throw new IllegalArgumentException("Required skills cannot be numbers only.");
        }
    }

    @Transactional
    public InterviewRoundResponse recordRoundResult(Long appId, Long roundId, RoundResultRequest req) {
        JobApplication app = appRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        
        InterviewRound round = roundRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("Round not found"));
        
        if (!round.getApplication().getId().equals(appId))
            throw new RuntimeException("Round does not belong to this application");

        String normalizedResult = req.getResult() != null ? req.getResult().trim().toUpperCase() : null;
        if (!"PASS".equals(normalizedResult) && !"FAIL".equals(normalizedResult)) {
            throw new RuntimeException("Round result must be PASS or FAIL.");
        }

        round.setResult(normalizedResult);
        round.setScore(req.getScore());
        round.setNotes(req.getNotes());
        round.setFeedback(null);
        round.setStatus(InterviewRound.RoundStatus.COMPLETED);

        InterviewRound updated = roundRepository.save(round);

        if ("FAIL".equals(normalizedResult)) {
            app.setFinalSelected(false);
            app.setInterviewResult("Rejected");
            app.setStatus(JobApplication.AppStatus.REJECTED);
        } else {
            // PASS: update status to reflect the round was cleared
            app.setStatus(getCompletedPassStatusForRoundType(round.getRoundType()));
        }
        appRepository.save(app);
        publishApplicationAction(app, UserActionType.ROUND_RESULT_RECORDED,
                "Your " + roundLabel(round) + " result for " + jobLabel(app.getJob())
                        + " has been recorded as " + normalizedResult
                        + (req.getScore() != null ? " with score " + req.getScore() + "." : "."));

        return toRoundResponse(updated);
    }

    @Transactional
    public ApplicationResponse finalizeRoundProcess(Long appId, FinalDecisionRequest req) {
        JobApplication app = appRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (app.getStatus() == JobApplication.AppStatus.SELECTED || app.getStatus() == JobApplication.AppStatus.REJECTED) {
            throw new RuntimeException("Final decision has already been recorded for this application.");
        }

        if (req.getFinalSelected() == null) {
            throw new RuntimeException("Please choose a final decision.");
        }

        List<InterviewRound> rounds = roundRepository.findByApplicationOrderByRoundNumberAsc(app);
        if (rounds.isEmpty()) {
            throw new RuntimeException("Schedule and complete at least one round before recording the final decision.");
        }

        InterviewRound latestRound = rounds.get(rounds.size() - 1);
        if (latestRound.getStatus() != InterviewRound.RoundStatus.COMPLETED) {
            throw new RuntimeException("Complete the latest scheduled round before recording the final decision.");
        }
        if (!"PASS".equalsIgnoreCase(latestRound.getResult())) {
            throw new RuntimeException("Final decision can be recorded only after the latest round is marked as PASS.");
        }
        app.setFinalSelected(req.getFinalSelected());
        app.setInterviewNotes(req.getNotes() != null && !req.getNotes().trim().isBlank() ? req.getNotes().trim() : null);
        app.setCurrentRound(latestRound.getRoundNumber());

        if (Boolean.TRUE.equals(req.getFinalSelected())) {
            app.setInterviewResult("Selected");
            app.setStatus(JobApplication.AppStatus.SELECTED);
        } else {
            app.setInterviewResult("Rejected");
            app.setStatus(JobApplication.AppStatus.REJECTED);
        }

        JobApplication saved = appRepository.save(app);
        // Send email AFTER save so a mail failure can never roll back the status change
        if (Boolean.TRUE.equals(req.getFinalSelected())) {
            emailService.sendFinalSelection(saved.getUser(), saved.getJob());
        } else {
            publishApplicationAction(saved, UserActionType.FINAL_DECISION_RECORDED,
                    "The final decision for your application to " + jobLabel(saved.getJob()) + " has been recorded as not selected.");
        }

        return toAppResponse(saved);
    }

    private void publishRoundScheduled(JobApplication app, InterviewRound round, boolean rescheduled) {
        publishApplicationAction(app, UserActionType.ROUND_SCHEDULED,
                "Your " + roundLabel(round) + " for " + jobLabel(app.getJob())
                        + " has been " + (rescheduled ? "rescheduled" : "scheduled")
                        + " on " + valueOrTbd(round.getScheduledDate() != null ? round.getScheduledDate().toString() : null)
                        + " at " + valueOrTbd(round.getScheduledTime())
                        + ". Mode: " + valueOrTbd(round.getInterviewMode())
                        + ". Place: " + valueOrTbd(round.getLocation()) + ".");
    }

    private void publishApplicationAction(JobApplication app, UserActionType actionType, String message) {
        User user = app.getUser();
        user.getEmail();
        user.getFirstName();
        eventPublisher.publishEvent(new UserActionEvent(user, actionType, message));
        log.info("Published application action event. applicationId={}, userId={}, action={}",
                app.getId(), user.getId(), actionType);
    }

    private String jobLabel(Job job) {
        if (job == null) return "the job";
        String title = job.getTitle() != null && !job.getTitle().isBlank() ? job.getTitle() : "the role";
        String company = job.getCompanyName() != null && !job.getCompanyName().isBlank() ? job.getCompanyName() : null;
        return company == null ? title : title + " at " + company;
    }

    private String roundLabel(InterviewRound round) {
        if (round == null) return "selection round";
        String type = round.getRoundType() != null && !round.getRoundType().isBlank()
                ? round.getRoundType()
                : "Round " + round.getRoundNumber();
        return type + " (Round " + round.getRoundNumber() + ")";
    }

    private String valueOrTbd(String value) {
        return value != null && !value.isBlank() ? value : "TBD";
    }

    private void validateRoundScheduleRequest(InterviewRoundRequest req) {
        if (req.getRoundType() == null || req.getRoundType().trim().isBlank()) {
            throw new RuntimeException("Round type is required.");
        }
        if (req.getInterviewMode() == null || req.getInterviewMode().trim().isBlank()) {
            throw new RuntimeException("Interview mode is required.");
        }
        if (req.getScheduledDate() == null || req.getScheduledDate().trim().isBlank()
                || req.getScheduledTime() == null || req.getScheduledTime().trim().isBlank()) {
            throw new RuntimeException("Round date and time are required.");
        }
        if (req.getLocation() == null || req.getLocation().trim().isBlank()) {
            throw new RuntimeException("Place / Location is required.");
        }
        if (!req.getLocation().trim().matches(ROUND_LOCATION_PATTERN)) {
            throw new RuntimeException("Place / Location can contain letters, numbers, spaces, and basic punctuation.");
        }
        if (!containsLetter(req.getLocation())) {
            throw new RuntimeException("Place / Location cannot contain only numbers.");
        }
        if (req.getDescription() == null || req.getDescription().trim().isBlank()) {
            throw new RuntimeException("Round description is required.");
        }
        if (!req.getDescription().trim().matches(ROUND_DESCRIPTION_PATTERN)) {
            throw new RuntimeException("Round description can contain letters, numbers, spaces, and basic punctuation.");
        }
        if (!containsLetter(req.getDescription())) {
            throw new RuntimeException("Round description cannot contain only numbers.");
        }

        try {
            LocalDate scheduledDate = LocalDate.parse(req.getScheduledDate().trim());
            LocalTime scheduledTime = LocalTime.parse(req.getScheduledTime().trim());
            LocalDateTime scheduledAt = LocalDateTime.of(scheduledDate, scheduledTime);
            if (!scheduledAt.isAfter(LocalDateTime.now())) {
                throw new RuntimeException("Past time is not allowed. Please choose a future round date and time.");
            }
        } catch (DateTimeParseException ex) {
            throw new RuntimeException("Please enter a valid round date and time.");
        }
    }

    private boolean containsLetter(String value) {
        return value != null && value.matches(".*[A-Za-z].*");
    }

    private InterviewRoundResponse toRoundResponse(InterviewRound r) {
        return InterviewRoundResponse.builder()
                .id(r.getId()).roundNumber(r.getRoundNumber()).roundType(r.getRoundType())
                .scheduledDate(r.getScheduledDate() != null ? r.getScheduledDate().toString() : null)
                .scheduledTime(r.getScheduledTime()).interviewMode(r.getInterviewMode())
                .location(r.getLocation()).description(r.getDescription())
                .status(r.getStatus().name()).result(r.getResult())
                .score(r.getScore()).notes(r.getNotes()).feedback(r.getFeedback())
                .build();
    }

    private void enrichCurrentRoundDetails(ApplicationResponse response, JobApplication application) {
        Optional<InterviewRound> latestRound = roundRepository.findByApplicationOrderByRoundNumberAsc(application)
                .stream()
                .reduce((first, second) -> second);

        if (latestRound.isEmpty()) return;

        InterviewRound round = latestRound.get();
        response.setCurrentRoundNumber(round.getRoundNumber());
        response.setCurrentRoundType(round.getRoundType());
        response.setCurrentRoundStatus(round.getStatus().name());

        if (application.getStatus() == JobApplication.AppStatus.SELECTED
                || application.getStatus() == JobApplication.AppStatus.REJECTED) {
            return;
        }

        normalizeAptitudeStatusFromRound(response, round);

        String roundName = round.getRoundType() != null && !round.getRoundType().isBlank()
                ? round.getRoundType()
                : "Round " + round.getRoundNumber();
        String roundState = getRoundDisplayState(round);

        response.setCurrentRoundDisplayStatus(roundName + " " + roundState);
    }

    private void normalizeAptitudeStatusFromRound(ApplicationResponse response, InterviewRound round) {
        if (!isAptitudeRound(round.getRoundType())) return;

        if (round.getStatus() == InterviewRound.RoundStatus.SCHEDULED) {
            response.setStatus(JobApplication.AppStatus.APTITUDE_SCHEDULED.name());
            return;
        }

        if (round.getStatus() == InterviewRound.RoundStatus.COMPLETED) {
            if ("PASS".equalsIgnoreCase(round.getResult())) {
                response.setStatus(JobApplication.AppStatus.APTITUDE_CLEARED.name());
            } else if ("FAIL".equalsIgnoreCase(round.getResult())) {
                response.setStatus(JobApplication.AppStatus.APTITUDE_FAILED.name());
            }
        }
    }

    private JobApplication.AppStatus getScheduledStatusForRoundType(String roundType) {
        return isAptitudeRound(roundType)
                ? JobApplication.AppStatus.APTITUDE_SCHEDULED
                : JobApplication.AppStatus.INTERVIEW_SCHEDULED;
    }

    private JobApplication.AppStatus getCompletedPassStatusForRoundType(String roundType) {
        // After passing an aptitude round → APTITUDE_CLEARED
        // After passing any interview/other round → stay at INTERVIEW_SCHEDULED
        // (signals "in the interview process, a round was cleared, awaiting next step")
        return isAptitudeRound(roundType)
                ? JobApplication.AppStatus.APTITUDE_CLEARED
                : JobApplication.AppStatus.INTERVIEW_SCHEDULED;
    }

    private boolean isAptitudeRound(String roundType) {
        return roundType != null && roundType.trim().toLowerCase().contains("aptitude");
    }

    private String getRoundDisplayState(InterviewRound round) {
        if (round.getStatus() == InterviewRound.RoundStatus.SCHEDULED) {
            return "Scheduled";
        }

        if (round.getStatus() == InterviewRound.RoundStatus.COMPLETED) {
            if ("PASS".equalsIgnoreCase(round.getResult())) {
                return isAptitudeRound(round.getRoundType()) ? "Cleared" : "Passed";
            }
            if ("FAIL".equalsIgnoreCase(round.getResult())) {
                return "Failed";
            }
        }

        return "Completed";
    }
}