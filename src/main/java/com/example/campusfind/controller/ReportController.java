package com.example.campusfind.controller;

import com.example.campusfind.dto.ReportForm;
import com.example.campusfind.model.CustodyStatus;
import com.example.campusfind.model.ItemCategory;
import com.example.campusfind.model.Report;
import com.example.campusfind.model.ReportStatus;
import com.example.campusfind.model.ReportType;
import com.example.campusfind.service.AccountService;
import com.example.campusfind.service.MatchingService;
import com.example.campusfind.service.ReportService;
import com.example.campusfind.service.SavedReportService;
import com.example.campusfind.service.MatchAlertService;
import com.example.campusfind.service.ImageStorageService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;

@Controller
public class ReportController {
    private final ReportService reportService;
    private final MatchingService matchingService;
    private final AccountService accountService;
    private final SavedReportService savedReportService;
    private final MatchAlertService matchAlertService;
    private final ImageStorageService imageStorageService;
    private final Clock clock;

    public ReportController(ReportService reportService, MatchingService matchingService, AccountService accountService,
                            SavedReportService savedReportService, MatchAlertService matchAlertService,
                            ImageStorageService imageStorageService, Clock clock) {
        this.reportService = reportService;
        this.matchingService = matchingService;
        this.accountService = accountService;
        this.savedReportService = savedReportService;
        this.matchAlertService = matchAlertService;
        this.imageStorageService = imageStorageService;
        this.clock = clock;
    }

    @InitBinder
    public void trimTextInputs(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(false));
    }

    @ModelAttribute("categories")
    public ItemCategory[] categories() { return ItemCategory.values(); }

    @ModelAttribute("reportTypes")
    public ReportType[] reportTypes() { return ReportType.values(); }

    @GetMapping("/reports")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String type,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        String safeType = validEnum(type, ReportType.class);
        String safeCategory = validEnum(category, ItemCategory.class);
        String safeStatus = "ALL".equalsIgnoreCase(status == null ? "" : status.trim()) ? "ALL" : validEnum(status, ReportStatus.class);
        Page<Report> reports = reportService.search(q, safeType, safeCategory, safeStatus, page);
        model.addAttribute("reports", reports);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("selectedType", safeType == null ? "" : safeType);
        model.addAttribute("selectedCategory", safeCategory == null ? "" : safeCategory);
        model.addAttribute("selectedStatus", safeStatus == null ? "OPEN" : safeStatus);
        model.addAttribute("statuses", ReportStatus.values());
        return "reports/list";
    }

    @GetMapping("/reports/new")
    public String newReport(@RequestParam(required = false) String type, Model model) {
        ReportForm form = new ReportForm();
        String safeType = validEnum(type, ReportType.class);
        if (safeType != null) form.setType(ReportType.valueOf(safeType));
        model.addAttribute("reportForm", form);
        model.addAttribute("custodyStatuses", CustodyStatus.values());
        return "reports/form";
    }

    @PostMapping("/reports")
    public String create(@Valid @ModelAttribute("reportForm") ReportForm form,
                         BindingResult bindingResult, Model model,
                         @RequestParam(value = "image", required = false) MultipartFile image,
                         @AuthenticationPrincipal UserDetails principal) {
        validateDate(form, bindingResult);

        String storedImageFileName = null;
        MultipartFile uploadFile = (image != null && !image.isEmpty()) ? image : form.getImage();
        if (uploadFile != null && !uploadFile.isEmpty()) {
            try {
                storedImageFileName = imageStorageService.storeImage(uploadFile);
            } catch (IllegalArgumentException e) {
                bindingResult.rejectValue("image", "image.invalid", e.getMessage());
                model.addAttribute("imageError", e.getMessage());
            } catch (Exception e) {
                bindingResult.rejectValue("image", "image.uploadFailed", "Could not upload image: " + e.getMessage());
                model.addAttribute("imageError", "Could not upload image: " + e.getMessage());
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("custodyStatuses", CustodyStatus.values());
            return "reports/form";
        }
        Report saved = reportService.create(form, storedImageFileName, accountService.findByEmail(principal.getUsername()));
        matchAlertService.notifyOwnersOfMatches(saved);
        return "redirect:/reports/" + saved.getId();
    }

    @GetMapping("/reports/{id}")
    public String detail(@PathVariable Long id, Model model,
                         @AuthenticationPrincipal UserDetails principal,
                         @RequestParam(required = false) String saved,
                         @RequestParam(required = false) String unsaved) {
        Report report = reportService.get(id);
        model.addAttribute("report", report);
        if (report.getStatus() == ReportStatus.OPEN) model.addAttribute("matches", matchingService.findMatches(report));
        if (principal != null) {
            var account = accountService.findByEmail(principal.getUsername());
            model.addAttribute("isSaved", savedReportService.isSaved(account, id));
            model.addAttribute("isOwner", report.getOwner() != null && report.getOwner().getId().equals(account.getId()));
        } else {
            model.addAttribute("isSaved", false);
            model.addAttribute("isOwner", false);
        }
        if (saved != null) model.addAttribute("noticeMessage", "Report saved to your bookmarks.");
        if (unsaved != null) model.addAttribute("noticeMessage", "Report removed from your bookmarks.");
        return "reports/detail";
    }

    @PostMapping("/reports/{id}/resolve")
    public String resolve(@PathVariable Long id, RedirectAttributes redirectAttributes,
                          @AuthenticationPrincipal UserDetails principal) {
        Report report = reportService.resolve(id, accountService.findByEmail(principal.getUsername()));
        if (report.getStatus() == ReportStatus.RESOLVED) redirectAttributes.addFlashAttribute("successMessage", "Report marked as resolved.");
        return "redirect:/reports/" + id;
    }

    private void validateDate(ReportForm form, BindingResult errors) {
        LocalDate date = form.getLastSeenOrFoundAt();
        if (date != null && (date.isAfter(LocalDate.now(clock)) || date.isBefore(LocalDate.now(clock).minusDays(365)))) {
            errors.rejectValue("lastSeenOrFoundAt", "date.outOfRange", "Date must be today or within the last 365 days.");
        }
    }

    private <E extends Enum<E>> String validEnum(String value, Class<E> enumType) {
        if (value == null || value.isBlank()) return null;
        try { return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT)).name(); }
        catch (IllegalArgumentException ignored) { return null; }
    }
}
