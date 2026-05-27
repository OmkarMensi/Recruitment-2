package com.git.recruitment.service;

import com.git.recruitment.event.UserActionEvent;
import com.git.recruitment.event.UserActionType;
import com.git.recruitment.model.*;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor @Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    @Value("${app.mail.from:${spring.mail.username}}") private String fromEmail;

    public void sendUserActionEmail(UserActionEvent event) {
        User user = event.getUser();
        send(user.getEmail(), subjectFor(event.getActionType()),
            base(iconBackgroundFor(event.getActionType()), iconColorFor(event.getActionType()), iconFor(event.getActionType()),
                "<h1 style='font-size:22px;color:#0f1b2d;margin:0 0 14px'>" + headingFor(event.getActionType()) + "</h1>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 14px'>Dear " + safe(user.getFirstName()) + ",</p>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 18px'>" + safe(event.getMessage()) + "</p>" +
                highlight(detailFor(event.getActionType())) +
                "<p style='font-size:13px;color:#999285;margin:24px 0 0'>Regards,<br><strong style='color:#0f1b2d'>Placement Office</strong><br>KLS Gogte Institute of Technology</p>"));
    }

    @Async public void sendRegistrationReceived(User u) {
        send(u.getEmail(), "Registration Received — GIT Recruitment Portal",
            base("#fff3cd","#b07a10", warningIcon(),
                "<h1 style='font-size:22px;color:#0f1b2d;margin:0 0 14px'>We've Received Your Registration</h1>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 14px'>Dear " + u.getFirstName() + ",</p>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 18px'>Thank you for registering on the <strong>GIT Campus Placement Portal</strong>. Your request is pending review by the Placement Office.</p>" +
                highlight("⏱ Approval typically takes <strong>1–2 business days</strong>. You will receive an email once reviewed.") +
                "<p style='font-size:13px;color:#999285;margin:24px 0 0'>Regards,<br><strong style='color:#0f1b2d'>Placement Office</strong><br>KLS Gogte Institute of Technology</p>"));
    }

    @Async public void sendRegistrationApproved(User u) {
        send(u.getEmail(), "Account Approved — GIT Recruitment Portal",
            base("#d8f3dc","#2d6a4f", checkIcon(),
                "<h1 style='font-size:22px;color:#0f1b2d;margin:0 0 14px'>Account Approved — You're In!</h1>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 14px'>Dear " + u.getFirstName() + ",</p>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 18px'>Your registration on the <strong>GIT Campus Placement Portal</strong> has been approved by the Placement Office.</p>" +
                highlight("✅ Log in to complete your profile, upload your CV, and apply for job opportunities.") +
                cta("Login to Portal", "http://localhost:3000/login") +
                "<p style='font-size:13px;color:#999285;margin:24px 0 0'>Warm regards,<br><strong style='color:#0f1b2d'>Placement Office</strong><br>KLS Gogte Institute of Technology</p>"));
    }

    @Async public void sendRegistrationRejected(User u) {
        send(u.getEmail(), "Update on Your Registration — GIT Recruitment Portal",
            base("#dce8f7","#1a3d6e", infoIcon(),
                "<h1 style='font-size:22px;color:#0f1b2d;margin:0 0 14px'>An Update on Your Registration</h1>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 14px'>Dear " + u.getFirstName() + ",</p>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 18px'>After review, we regret that your registration could not be approved at this time. Please contact the Placement Office for clarification.</p>" +
                "<p style='font-size:13px;color:#999285;margin:24px 0 0'>Regards,<br><strong style='color:#0f1b2d'>Placement Office</strong></p>"));
    }

    @Async public void sendCVShortlisted(User u) {
        send(u.getEmail(), "Congratulations — Your CV Has Been Shortlisted!",
            base("#d8f3dc","#2d6a4f", starIcon(),
                "<h1 style='font-size:22px;color:#0f1b2d;margin:0 0 14px'>Congratulations, " + u.getFirstName() + "! Your CV Is Shortlisted!</h1>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 14px'>Dear " + u.getFirstName() + ",</p>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 18px'>Your CV has been reviewed and shortlisted. Your profile is now being forwarded to our recruiting partner companies.</p>" +
                highlight("🎉 CV shared with companies for the <strong>2025-26 Placement Season</strong>. Interview calls will follow.") +
                cta("View My Application", "http://localhost:3000/student/status") +
                "<p style='font-size:13px;color:#999285;margin:24px 0 0'>Best wishes,<br><strong style='color:#0f1b2d'>Placement Office</strong></p>"));
    }

    @Async public void sendCVRejected(User u) {
        send(u.getEmail(), "Update on Your CV Application — GIT Recruitment Portal",
            base("#dce8f7","#1a3d6e", infoIcon(),
                "<h1 style='font-size:22px;color:#0f1b2d;margin:0 0 14px'>An Update on Your CV Application</h1>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 14px'>Dear " + u.getFirstName() + ",</p>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 18px'>After careful review, your application has not been shortlisted for the current cycle. We encourage you to update your profile and reapply next season.</p>" +
                "<p style='font-size:13px;color:#999285;margin:24px 0 0'>Regards,<br><strong style='color:#0f1b2d'>Placement Office</strong></p>"));
    }

    @Async public void sendJobApplicationConfirmation(User u, Job j) {
        send(u.getEmail(), "Application Submitted — " + j.getCompanyName(),
            base("#d8f3dc","#2d6a4f", checkIcon(),
                "<h1 style='font-size:22px;color:#0f1b2d;margin:0 0 14px'>Application Submitted Successfully!</h1>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 14px'>Dear " + u.getFirstName() + ",</p>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 18px'>Your application for <strong>" + j.getTitle() + "</strong> at <strong>" + j.getCompanyName() + "</strong> has been received successfully.</p>" +
                highlight("📋 Job: " + j.getTitle() + "<br>🏢 Company: " + j.getCompanyName() + "<br>📍 Location: " + (j.getLocation() != null ? j.getLocation() : "TBD")) +
                cta("Track Application", "http://localhost:3000/student/applications") +
                "<p style='font-size:13px;color:#999285;margin:24px 0 0'>Best wishes,<br><strong style='color:#0f1b2d'>Placement Office</strong></p>"));
    }

    @Async public void sendInterviewScheduled(User u, Job j, JobApplication app) {
        send(u.getEmail(), "Interview Scheduled — " + j.getCompanyName(),
            base("#fff3cd","#b07a10", warningIcon(),
                "<h1 style='font-size:22px;color:#0f1b2d;margin:0 0 14px'>Your Interview Has Been Scheduled!</h1>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 14px'>Dear " + u.getFirstName() + ",</p>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 18px'>Congratulations! You have been selected for an interview for <strong>" + j.getTitle() + "</strong> at <strong>" + j.getCompanyName() + "</strong>.</p>" +
                highlight("📅 Date: <strong>" + app.getInterviewDate() + "</strong><br>⏰ Time: <strong>" + app.getInterviewTime() + "</strong><br>📍 Mode: <strong>" + app.getInterviewMode() + "</strong>") +
                "<p style='font-size:14px;color:#5a5550;line-height:1.75;margin:18px 0'>Please be prepared and arrive on time. All the best!</p>" +
                cta("View Details", "http://localhost:3000/student/applications") +
                "<p style='font-size:13px;color:#999285;margin:24px 0 0'>Best wishes,<br><strong style='color:#0f1b2d'>Placement Office</strong></p>"));
    }

    public void sendNewJobNotification(User u, Job j) {
        send(u.getEmail(), "New Job Added - " + safe(j.getCompanyName()),
            base("#fff3cd","#b07a10", warningIcon(),
                "<h1 style='font-size:22px;color:#0f1b2d;margin:0 0 14px'>New Job Added</h1>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 14px'>Dear " + safe(u.getFirstName()) + ",</p>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 18px'><strong>" + safe(j.getTitle()) + "</strong> role at <strong>" + safe(j.getCompanyName()) + "</strong> is added by your placement office. Please login and check more details.</p>" +
                "<p style='font-size:13px;color:#999285;margin:24px 0 0'>Regards,<br><strong style='color:#0f1b2d'>Placement Office</strong><br>KLS Gogte Institute of Technology</p>"));
    }

    public void sendRegistrationOtp(String email, String otp) {
        send(email, "Registration OTP - GIT Recruitment Portal",
            base("#fff3cd","#b07a10", warningIcon(),
                "<h1 style='font-size:22px;color:#0f1b2d;margin:0 0 14px'>Verify Your Email</h1>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 14px'>Use this OTP to verify your student email before registration.</p>" +
                "<div style='font-size:28px;font-weight:700;letter-spacing:6px;color:#0f1b2d;background:#f5ecd4;border-radius:8px;padding:16px;text-align:center;margin:18px 0'>" + safe(otp) + "</div>" +
                "<p style='font-size:14px;color:#5a5550;line-height:1.75;margin:0'>This OTP is valid for 5 minutes.</p>" +
                "<p style='font-size:13px;color:#999285;margin:24px 0 0'>Regards,<br><strong style='color:#0f1b2d'>Placement Office</strong><br>KLS Gogte Institute of Technology</p>"));
    }

    @Async public void sendFinalSelection(User u, Job j) {
        send(u.getEmail(), "🎉 Congratulations — Selected at " + j.getCompanyName() + "!",
            base("#d8f3dc","#2d6a4f", starIcon(),
                "<h1 style='font-size:22px;color:#0f1b2d;margin:0 0 14px'>Congratulations — You Are Selected!</h1>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 14px'>Dear " + u.getFirstName() + ",</p>" +
                "<p style='font-size:14px;color:#3d3830;line-height:1.75;margin:0 0 18px'>We are delighted to inform you that you have been <strong>finally selected</strong> for the position of <strong>" + j.getTitle() + "</strong> at <strong>" + j.getCompanyName() + "</strong>. The Placement Office will contact you with further details shortly.</p>" +
                highlight("🎉 You have been placed! Congratulations on this achievement. The Placement Office is proud of you!") +
                "<p style='font-size:13px;color:#999285;margin:24px 0 0'>With best wishes,<br><strong style='color:#0f1b2d'>Placement Office</strong><br>KLS Gogte Institute of Technology</p>"));
    }

    private void send(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(fromEmail, "GIT Placement Office");
            h.setTo(to); h.setSubject(subject); h.setText(html, true);
            mailSender.send(msg);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            // Log and swallow: email failures must never roll back a business transaction.
            // All callers are either @Async or fire-and-forget; propagating here would
            // silently undo status changes (e.g. candidate SELECTED rolling back).
            log.error("Email failed to {}: {}", to, e.getMessage(), e);
        }
    }

    private String subjectFor(UserActionType actionType) {
        return switch (actionType) {
            case REGISTRATION_APPROVED -> "Account Approved - GIT Recruitment Portal";
            case REGISTRATION_REJECTED -> "Registration Update - GIT Recruitment Portal";
            case CV_SHORTLISTED -> "Congratulations - Your CV Has Been Shortlisted";
            case CV_REJECTED -> "CV Review Update - GIT Recruitment Portal";
            case PROFILE_UNDER_REVIEW -> "Profile Under Review - GIT Recruitment Portal";
            case PROFILE_UPDATED -> "Profile Updated - GIT Recruitment Portal";
            case PROFILE_DELETED -> "Profile Deleted - GIT Recruitment Portal";
            case APTITUDE_SCHEDULED -> "Aptitude Round Scheduled - GIT Recruitment Portal";
            case APTITUDE_RESULT_RECORDED -> "Aptitude Result Updated - GIT Recruitment Portal";
            case INTERVIEW_SCHEDULED -> "Interview Scheduled - GIT Recruitment Portal";
            case ROUND_SCHEDULED -> "Selection Round Scheduled - GIT Recruitment Portal";
            case ROUND_RESULT_RECORDED -> "Selection Round Result Updated - GIT Recruitment Portal";
            case APPLICATION_REJECTED -> "Application Update - GIT Recruitment Portal";
            case FINAL_DECISION_RECORDED -> "Final Decision Updated - GIT Recruitment Portal";
        };
    }

    private String headingFor(UserActionType actionType) {
        return switch (actionType) {
            case REGISTRATION_APPROVED -> "Account Approved";
            case REGISTRATION_REJECTED -> "Registration Update";
            case CV_SHORTLISTED -> "CV Shortlisted";
            case CV_REJECTED -> "CV Review Update";
            case PROFILE_UNDER_REVIEW -> "Profile Under Review";
            case PROFILE_UPDATED -> "Profile Updated";
            case PROFILE_DELETED -> "Profile Deleted";
            case APTITUDE_SCHEDULED -> "Aptitude Round Scheduled";
            case APTITUDE_RESULT_RECORDED -> "Aptitude Result Updated";
            case INTERVIEW_SCHEDULED -> "Interview Scheduled";
            case ROUND_SCHEDULED -> "Selection Round Scheduled";
            case ROUND_RESULT_RECORDED -> "Selection Round Result Updated";
            case APPLICATION_REJECTED -> "Application Update";
            case FINAL_DECISION_RECORDED -> "Final Decision Updated";
        };
    }

    private String detailFor(UserActionType actionType) {
        return switch (actionType) {
            case REGISTRATION_APPROVED -> "You can now log in, complete your profile, upload your CV, and apply for eligible jobs.";
            case REGISTRATION_REJECTED -> "Please contact the Placement Office if you need clarification or want to correct your details.";
            case CV_SHORTLISTED -> "Your profile will now move forward in the placement process.";
            case CV_REJECTED -> "You may update your profile and CV before the next eligible placement cycle.";
            case PROFILE_UNDER_REVIEW -> "You will receive another update once the review is completed.";
            case PROFILE_UPDATED -> "Please log in and verify your latest profile details.";
            case PROFILE_DELETED -> "If you believe this was done by mistake, contact the Placement Office immediately.";
            case APTITUDE_SCHEDULED -> "Please log in to the portal and check your application details before the scheduled time.";
            case APTITUDE_RESULT_RECORDED -> "Your aptitude result has been updated in the portal.";
            case INTERVIEW_SCHEDULED -> "Please review the interview details and be available at the scheduled time.";
            case ROUND_SCHEDULED -> "Please review the round details and prepare accordingly.";
            case ROUND_RESULT_RECORDED -> "Your latest round result has been updated in the portal.";
            case APPLICATION_REJECTED -> "Please contact the Placement Office if you need clarification about this update.";
            case FINAL_DECISION_RECORDED -> "Please log in to the portal to review your final application status.";
        };
    }

    private String iconBackgroundFor(UserActionType actionType) {
        return switch (actionType) {
            case REGISTRATION_APPROVED, CV_SHORTLISTED, FINAL_DECISION_RECORDED -> "#d8f3dc";
            case REGISTRATION_REJECTED, CV_REJECTED, PROFILE_DELETED, APPLICATION_REJECTED -> "#dce8f7";
            case PROFILE_UNDER_REVIEW, PROFILE_UPDATED, APTITUDE_SCHEDULED, APTITUDE_RESULT_RECORDED, INTERVIEW_SCHEDULED, ROUND_SCHEDULED, ROUND_RESULT_RECORDED -> "#fff3cd";
        };
    }

    private String iconColorFor(UserActionType actionType) {
        return switch (actionType) {
            case REGISTRATION_APPROVED, CV_SHORTLISTED, FINAL_DECISION_RECORDED -> "#2d6a4f";
            case REGISTRATION_REJECTED, CV_REJECTED, PROFILE_DELETED, APPLICATION_REJECTED -> "#1a3d6e";
            case PROFILE_UNDER_REVIEW, PROFILE_UPDATED, APTITUDE_SCHEDULED, APTITUDE_RESULT_RECORDED, INTERVIEW_SCHEDULED, ROUND_SCHEDULED, ROUND_RESULT_RECORDED -> "#b07a10";
        };
    }

    private String iconFor(UserActionType actionType) {
        return switch (actionType) {
            case REGISTRATION_APPROVED -> checkIcon();
            case CV_SHORTLISTED, FINAL_DECISION_RECORDED -> starIcon();
            case PROFILE_UNDER_REVIEW, PROFILE_UPDATED, APTITUDE_SCHEDULED, APTITUDE_RESULT_RECORDED, INTERVIEW_SCHEDULED, ROUND_SCHEDULED, ROUND_RESULT_RECORDED -> warningIcon();
            case REGISTRATION_REJECTED, CV_REJECTED, PROFILE_DELETED, APPLICATION_REJECTED -> infoIcon();
        };
    }

    private String safe(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String base(String iconBg, String iconColor, String icon, String content) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>" +
            "<body style='margin:0;padding:0;background:#f0ede6;font-family:Segoe UI,Arial,sans-serif;'>" +
            "<table width='100%' cellpadding='0' cellspacing='0' style='padding:32px 16px;'><tr><td align='center'>" +
            "<table width='580' cellpadding='0' cellspacing='0' style='max-width:580px;width:100%;'>" +
            "<tr><td style='background:#0f1b2d;border-radius:16px 16px 0 0;padding:28px 36px 24px;text-align:center;'>" +
            "<div style='font-size:20px;font-weight:700;color:#e8c97a;'>GIT Campus Placement Portal</div>" +
            "<div style='font-size:11px;color:rgba(255,255,255,0.4);letter-spacing:1px;margin-top:4px;'>KLS GOGTE INSTITUTE OF TECHNOLOGY</div>" +
            "<div style='width:60px;height:60px;border-radius:50%;background:" + iconBg + ";margin:20px auto 0;display:flex;align-items:center;justify-content:center;'>" + icon + "</div>" +
            "</td></tr>" +
            "<tr><td style='background:#fff;padding:32px 36px;'>" + content + "</td></tr>" +
            "<tr><td style='background:#f0ede6;border-radius:0 0 16px 16px;padding:20px 36px;text-align:center;border-top:1px solid #ddd9d0;'>" +
            "<p style='margin:0;font-size:12px;color:#999285;'>Placement Office &bull; KLS Gogte Institute of Technology<br>" +
            "<a href='mailto:placements@git.edu' style='color:#0f1b2d;'>placements@git.edu</a> &bull; +91 80000 00000</p>" +
            "<p style='margin:8px 0 0;font-size:10px;color:#b4b2a9;'>This is an automated message. Do not reply directly.</p>" +
            "</td></tr></table></td></tr></table></body></html>";
    }

    private String highlight(String text) {
        return "<div style='background:#f5ecd4;border-left:4px solid #c9a84c;border-radius:0 8px 8px 0;padding:14px 18px;margin:18px 0;font-size:13px;color:#3d3830;line-height:1.7;'>" + text + "</div>";
    }

    private String cta(String label, String url) {
        return "<div style='text-align:center;margin:24px 0;'><a href='" + url + "' style='background:#0f1b2d;color:#fff;text-decoration:none;padding:13px 32px;border-radius:10px;font-size:14px;font-weight:600;display:inline-block;'>" + label + " →</a></div>";
    }

    private String checkIcon() { return "<svg width='28' height='28' viewBox='0 0 24 24' fill='#2d6a4f'><path d='M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z'/></svg>"; }
    private String warningIcon() { return "<svg width='28' height='28' viewBox='0 0 24 24' fill='#b07a10'><path d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z'/></svg>"; }
    private String infoIcon() { return "<svg width='28' height='28' viewBox='0 0 24 24' fill='#1a3d6e'><path d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z'/></svg>"; }
    private String starIcon() { return "<svg width='28' height='28' viewBox='0 0 24 24' fill='#c9a84c'><path d='M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z'/></svg>"; }
}