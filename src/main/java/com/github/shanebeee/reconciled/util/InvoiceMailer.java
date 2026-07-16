package com.github.shanebeee.reconciled.util;

import com.github.shanebeee.reconciled.model.Boss;
import com.github.shanebeee.reconciled.model.EmployeeInfo;
import com.github.shanebeee.reconciled.model.Invoice;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Composes a new Mail.app message with the invoice PDF attached via AppleScript.
 * Opens Mail with everything pre-filled — user just reviews and hits Send.
 */
public class InvoiceMailer {

    /**
     * Opens Mail.app with a pre-composed message for the given invoice.
     *
     * @param invoice  The invoice record (for subject, numbers, dates)
     * @param boss     The boss (for email address and name)
     * @param employee The employee (for sign-off name)
     * @throws IOException           If the AppleScript execution fails
     * @throws IllegalStateException If the PDF file doesn't exist or boss has no email
     */
    public static void composeEmail(Invoice invoice, Boss boss, EmployeeInfo employee)
        throws IOException, IllegalStateException {

        // Validate
        if (boss.getEmail() == null || boss.getEmail().isBlank()) {
            throw new IllegalStateException("No email address set for " + boss.getName()
                + ".\nAdd one in Boss Management first.");
        }
        File pdf = new File(invoice.getPdfPath());
        if (!pdf.exists()) {
            throw new IllegalStateException("PDF not found at:\n" + invoice.getPdfPath()
                + "\n\nRegenerate the invoice first.");
        }

        // Format period for subject line
        String period = formatPeriod(invoice.getStartDate(), invoice.getEndDate());
        String subject = "Invoice #" + invoice.getInvoiceNumber() + " \u2014 " + period;

        // Build greeting name
        String firstName = boss.getName().split(" ")[0];

        // Sign-off name
        String senderName = (employee != null && employee.getFullName() != null
            && !employee.getFullName().isBlank())
            ? employee.getFullName() : "Shane";

        String body = "Hi " + firstName + ",\\n\\n"
            + "Please find attached Invoice #" + invoice.getInvoiceNumber()
            + " for " + period + ", totalling $"
            + String.format("%.2f", invoice.getTotalAmount()) + " (including GST).\\n\\n"
            + "Please don't hesitate to reach out if you have any questions.\\n\\n"
            + "Thanks,\\n"
            + senderName;

        // Escape the PDF path for AppleScript (handle spaces)
        String posixPath = pdf.getAbsolutePath();

        String script = "tell application \"Mail\"\n"
            + "    set newMsg to make new outgoing message with properties "
            + "{subject:\"" + escapeAppleScript(subject) + "\", "
            + "content:\"" + body + "\", "
            + "visible:true}\n"
            + "    tell newMsg\n"
            + "        make new to recipient with properties "
            + "{address:\"" + escapeAppleScript(boss.getEmail()) + "\"}\n"
            + "        make new attachment with properties "
            + "{file name:POSIX file \"" + escapeAppleScript(posixPath) + "\"}\n"
            + "    end tell\n"
            + "    activate\n"
            + "end tell";

        Runtime.getRuntime().exec(new String[]{"osascript", "-e", script});
    }

    private static String formatPeriod(String start, String end) {
        if (start == null || end == null) return "";
        try {
            LocalDate s = LocalDate.parse(start);
            LocalDate e = LocalDate.parse(end);
            if (s.getMonth() == e.getMonth() && s.getYear() == e.getYear()) {
                return s.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
            }
            return s.format(DateTimeFormatter.ofPattern("MMM d"))
                + " \u2013 " + e.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        } catch (Exception ex) {
            return start + " \u2013 " + end;
        }
    }

    private static String escapeAppleScript(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

}
