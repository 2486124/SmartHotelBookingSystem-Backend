package com.cts.project.shbs.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // ── Public API ─────────────────────────────────────────────────────────

    public void sendBookingConfirmationEmail(String toEmail, Long bookingId, String hotelName,
                                              String checkIn, String checkOut,
                                              Double amount, String paymentMethod, boolean redeemPoints) {
        String ref = String.format("HV-%05d", bookingId);
        sendHtml(toEmail,
                "Booking Confirmed - HotelVerse " + ref,
                confirmationHtml(ref, hotelName, checkIn, checkOut, amount, paymentMethod, redeemPoints));
    }

    public void sendCancellationEmail(String toEmail, Long bookingId, String hotelName,
                                       String checkIn, String checkOut, Double refundAmount) {
        String ref = String.format("HV-%05d", bookingId);
        sendHtml(toEmail,
                "Booking Cancelled - HotelVerse " + ref,
                cancellationHtml(ref, hotelName, checkIn, checkOut, refundAmount));
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        sendHtml(toEmail, "Reset Your Password — HotelVerse", passwordResetHtml(resetLink));
    }

    // ── HTML builders ──────────────────────────────────────────────────────

    private String confirmationHtml(String ref, String hotel, String checkIn, String checkOut,
                                     Double amount, String method, boolean redeemed) {
        String loyaltyRow = redeemed
                ? row("Loyalty Discount",
                      "<span style='color:#c8960c;font-weight:700;'>300 pts redeemed &mdash; 10% discount applied &#9733;</span>")
                : "";

        String details =
                "<table width='100%' cellpadding='0' cellspacing='0'>" +
                row("Hotel",          hotel)   +
                row("Check-In",       checkIn) +
                row("Check-Out",      checkOut) +
                row("Payment Method", method)  +
                loyaltyRow +
                "</table>" +
                totalBox("Total Amount Paid", amount) +
                note("Please present your booking reference <strong style='color:#1a237e;'>#" + ref +
                     "</strong> at the front desk on arrival. If you have any questions, our support team is happy to help.");

        return wrap(
                header("BOOKING CONFIRMATION") +
                banner("#e8f5e9", "#c8e6c9", "#2e7d32", "#1b5e20",
                       "&#10003;", "Booking Confirmed!", "Your stay has been successfully reserved.") +
                body(refPill(ref) + details) +
                footer());
    }

    private String cancellationHtml(String ref, String hotel, String checkIn, String checkOut,
                                     Double refund) {
        boolean hasRefund = refund != null && refund > 0;

        String refundRow = hasRefund
                ? row("Refund Amount",
                      "<span style='color:#2e7d32;font-weight:700;'>&#8377;" +
                      String.format("%.2f", refund) + " (initiated)</span>")
                : row("Refund", "Not applicable");

        String refundNote = hasRefund
                ? "<p style='margin:14px 0 0;font-size:13px;color:#33691e;line-height:1.7;" +
                  "background:#f1f8e9;border-radius:8px;padding:12px 16px;border-left:3px solid #7cb342;'>" +
                  "Your refund has been initiated and should reflect within " +
                  "<strong>5&ndash;7 business days</strong>.</p>"
                : "";

        String details =
                "<table width='100%' cellpadding='0' cellspacing='0'>" +
                row("Hotel",      hotel)   +
                row("Check-In",  checkIn)  +
                row("Check-Out", checkOut) +
                refundRow +
                "</table>" +
                refundNote +
                note("If you did not request this cancellation, please " +
                     "<strong style='color:#c62828;'>contact our support team immediately</strong>.");

        return wrap(
                header("CANCELLATION NOTICE") +
                banner("#fff3e0", "#ffe0b2", "#e65100", "#bf360c",
                       "&#10005;", "Booking Cancelled", "Your booking has been cancelled.") +
                body(refPill(ref) + details) +
                footer());
    }

    private String passwordResetHtml(String resetLink) {
        String content =
                "<p style='font-size:15px;color:#1a1a2e;line-height:1.75;margin:0 0 24px;'>" +
                "Click the button below to set a new password for your HotelVerse account. " +
                "This link is valid for <strong style='color:#1a237e;'>15 minutes</strong> only." +
                "</p>" +

                // CTA button — full-width on mobile via .cta-wrap / .cta-btn classes
                "<table width='100%' cellpadding='0' cellspacing='0' style='margin:0 0 24px;'>" +
                "<tr><td class='cta-wrap' align='center'>" +
                "<a href='" + resetLink + "' class='cta-btn' " +
                "style='display:inline-block;background:#1a237e;color:#ffffff;font-size:15px;" +
                "font-weight:700;padding:14px 40px;border-radius:10px;text-decoration:none;" +
                "letter-spacing:0.03em;'>Reset My Password &rarr;</a>" +
                "</td></tr></table>" +

                // Paste-link fallback
                "<p style='font-size:12px;color:#9090a8;line-height:1.6;margin:0 0 16px;" +
                "word-break:break-all;'>" +
                "Or paste this link into your browser:<br>" +
                "<span style='color:#1a237e;'>" + resetLink + "</span>" +
                "</p>" +

                // Warning note
                "<p style='font-size:13px;color:#6b4e00;line-height:1.7;background:#fff8e1;" +
                "border-radius:8px;padding:12px 16px;border-left:3px solid #c8960c;margin:0;'>" +
                "If you did not request a password reset, you can safely ignore this email &mdash; " +
                "your account has not been compromised." +
                "</p>";

        return wrap(
                header("PASSWORD RESET") +
                banner("#eef0fb", "#c8cef0", "#1a237e", "#0d1462",
                       "&#128274;", "Password Reset Request",
                       "We received a request to reset your account password.") +
                body(content) +
                footer());
    }

    // ── Layout helpers ─────────────────────────────────────────────────────

    /**
     * Outermost email wrapper.
     * Includes a <style> block with media-query overrides for mobile (max-width 600px).
     * Inline styles handle the desktop baseline; classes allow mobile to override via !important.
     */
    private String wrap(String content) {
        String style =
                "<style type='text/css'>" +
                "  body { margin:0; padding:0; background:#f5f6fa; }" +
                "  @media only screen and (max-width:600px) {" +
                // Card: full-width, no rounded corners so it fills the screen edge-to-edge
                "    .card          { width:100% !important; border-radius:0 !important; }" +
                // Reduce horizontal padding on all sections
                "    .header-cell  { padding:18px 20px !important; }" +
                "    .banner-cell  { padding:18px 20px !important; }" +
                "    .body-cell    { padding:22px 20px !important; }" +
                "    .footer-cell  { padding:16px 20px !important; }" +
                // Slightly smaller logo on narrow screens
                "    .logo         { font-size:18px !important; }" +
                // Header tag label: hide on very small screens to avoid wrapping
                "    .header-tag   { display:none !important; }" +
                // Banner title a bit smaller
                "    .banner-title { font-size:15px !important; }" +
                // CTA button: stretch full width on mobile
                "    .cta-wrap     { text-align:left !important; }" +
                "    .cta-btn      { display:block !important; text-align:center !important;" +
                "                   padding:14px 20px !important; }" +
                // Total-amount figure: slightly smaller
                "    .total-amount { font-size:19px !important; }" +
                // Row text: same size is fine, but tighten padding
                "    .detail-td    { padding:9px 0 !important; font-size:13px !important; }" +
                "  }" +
                "</style>";

        return "<!DOCTYPE html><html lang='en'>" +
               "<head><meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
               style +
               "</head>" +
               "<body style='margin:0;padding:0;background:#f5f6fa;" +
               "font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,Arial,sans-serif;'>" +

               "<table width='100%' cellpadding='0' cellspacing='0' " +
               "style='background:#f5f6fa;padding:36px 16px;'>" +
               "<tr><td align='center'>" +

               // White card — .card class enables mobile border-radius override
               "<table class='card' width='100%' cellpadding='0' cellspacing='0' " +
               "style='max-width:580px;background:#ffffff;border-radius:16px;overflow:hidden;" +
               "box-shadow:0 4px 24px rgba(26,35,126,0.10);'>" +
               content +
               "</table>" +

               "</td></tr>" +

               // Copyright line
               "<tr><td align='center' style='padding-top:14px;'>" +
               "<p style='font-size:12px;color:#b0b0c8;margin:0;'>" +
               "&copy; 2026 HotelVerse. All rights reserved." +
               "</p></td></tr>" +

               "</table></body></html>";
    }

    /** Navy top bar with HotelVerse logo and email-type label. */
    private String header(String tag) {
        return "<tr>" +
               "<td class='header-cell' style='background:#1a237e;padding:24px 32px;'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +

               "<td><span class='logo' style='font-size:22px;font-weight:800;color:#ffffff;" +
               "letter-spacing:-0.5px;'>Hotel<span style='color:#c8960c;'>Verse</span></span></td>" +

               "<td class='header-tag' align='right'>" +
               "<span style='font-size:11px;color:rgba(255,255,255,0.55);font-weight:600;" +
               "letter-spacing:0.09em;text-transform:uppercase;'>" + tag + "</span>" +
               "</td>" +

               "</tr></table></td></tr>";
    }

    /** Coloured status banner — green / orange / blue depending on email type. */
    private String banner(String bg, String borderColor, String iconBg, String titleColor,
                           String icon, String title, String sub) {
        return "<tr>" +
               "<td class='banner-cell' style='background:" + bg + ";padding:22px 32px;" +
               "border-bottom:1px solid " + borderColor + ";'>" +
               "<table cellpadding='0' cellspacing='0'><tr>" +

               // Circle icon
               "<td style='width:46px;height:46px;min-width:46px;background:" + iconBg + ";" +
               "border-radius:50%;text-align:center;vertical-align:middle;'>" +
               "<span style='color:#fff;font-size:20px;line-height:46px;display:block;'>" + icon + "</span>" +
               "</td>" +

               // Title + subtitle
               "<td style='padding-left:14px;vertical-align:middle;'>" +
               "<div class='banner-title' style='font-size:17px;font-weight:800;color:" + titleColor + ";'>" + title + "</div>" +
               "<div style='font-size:13px;color:#6a6a8a;margin-top:3px;'>" + sub + "</div>" +
               "</td>" +

               "</tr></table></td></tr>";
    }

    /** White body content area. */
    private String body(String content) {
        return "<tr><td class='body-cell' style='padding:28px 32px;'>" + content + "</td></tr>";
    }

    /** Blue booking-reference pill. */
    private String refPill(String ref) {
        return "<div style='display:inline-block;background:#eef0fb;border:1px solid #c8cef0;" +
               "border-radius:8px;padding:8px 16px;margin-bottom:22px;'>" +
               "<span style='font-size:11px;color:#9090a8;font-weight:600;letter-spacing:0.07em;" +
               "text-transform:uppercase;'>Booking Reference &nbsp;</span>" +
               "<span style='font-size:14px;color:#1a237e;font-weight:800;'>" + ref + "</span>" +
               "</div>";
    }

    /** Two-column label / value detail row. */
    private String row(String label, String value) {
        return "<tr>" +
               "<td class='detail-td' style='padding:11px 0;border-bottom:1px solid #f5f5fa;" +
               "font-size:13px;color:#9090a8;font-weight:500;width:45%;'>" + label + "</td>" +
               "<td class='detail-td' style='padding:11px 0;border-bottom:1px solid #f5f5fa;" +
               "font-size:13px;color:#1a1a2e;font-weight:600;text-align:right;'>" + value + "</td>" +
               "</tr>";
    }

    /** Navy total-amount banner at the bottom of the detail section. */
    private String totalBox(String label, Double amount) {
        return "<table width='100%' cellpadding='0' cellspacing='0' " +
               "style='margin-top:18px;background:#1a237e;border-radius:12px;'><tr>" +
               "<td style='padding:15px 20px;font-size:13px;font-weight:600;" +
               "color:rgba(255,255,255,0.7);text-transform:uppercase;letter-spacing:0.06em;'>" +
               label + "</td>" +
               "<td align='right' style='padding:15px 20px;'>" +
               "<span class='total-amount' style='font-size:22px;font-weight:800;color:#fff;'>" +
               "&#8377;" + String.format("%.2f", amount) + "</span></td>" +
               "</tr></table>";
    }

    /** Muted note paragraph. */
    private String note(String html) {
        return "<p style='margin:20px 0 0;font-size:13px;color:#9090a8;line-height:1.75;'>" +
               html + "</p>";
    }

    /** Light grey footer with support email and disclaimer. */
    private String footer() {
        return "<tr>" +
               "<td class='footer-cell' style='background:#f8f8fc;border-top:1px solid #e4e4ef;" +
               "padding:20px 32px;text-align:center;'>" +
               "<p style='margin:0 0 5px;font-size:13px;color:#9090a8;'>" +
               "Questions? Reach us at " +
               "<a href='mailto:support@hotelverse.com' " +
               "style='color:#1a237e;font-weight:600;text-decoration:none;'>" +
               "support@hotelverse.com</a></p>" +
               "<p style='margin:0;font-size:12px;color:#b0b0c8;'>" +
               "This is an automated message &mdash; please do not reply directly to this email." +
               "</p></td></tr>";
    }

    // ── Send helper ────────────────────────────────────────────────────────

    private void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mime);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
