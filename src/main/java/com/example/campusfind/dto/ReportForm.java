package com.example.campusfind.dto;

import com.example.campusfind.model.CustodyStatus;
import com.example.campusfind.model.ItemCategory;
import com.example.campusfind.model.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.web.multipart.MultipartFile;

public class ReportForm {
    @NotNull(message = "Choose whether you lost or found the item.")
    private ReportType type;

    @NotBlank(message = "Enter an item name.")
    @Size(min = 3, max = 80, message = "Item name must be between 3 and 80 characters.")
    private String itemName;

    @NotNull(message = "Choose a category.")
    private ItemCategory category;

    @NotBlank(message = "Add a short description.")
    @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters.")
    private String description;

    @NotNull(message = "Choose the date you last saw or found it.")
    private LocalDate lastSeenOrFoundAt;

    @NotBlank(message = "Enter a campus location.")
    @Size(min = 2, max = 100, message = "Location must be between 2 and 100 characters.")
    private String location;

    private CustodyStatus custodyStatus = CustodyStatus.WITH_FINDER;

    @Size(max = 200, message = "Proof of ownership question must not exceed 200 characters.")
    private String secretQuestion;

    @Size(max = 100, message = "Reward must not exceed 100 characters.")
    private String reward;

    @Size(max = 50, message = "Student ID must not exceed 50 characters.")
    private String studentId;

    private MultipartFile image;

    public ReportType getType() { return type; }
    public void setType(ReportType type) { this.type = type; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getLastSeenOrFoundAt() { return lastSeenOrFoundAt; }
    public void setLastSeenOrFoundAt(LocalDate lastSeenOrFoundAt) { this.lastSeenOrFoundAt = lastSeenOrFoundAt; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public CustodyStatus getCustodyStatus() { return custodyStatus != null ? custodyStatus : CustodyStatus.WITH_FINDER; }
    public void setCustodyStatus(CustodyStatus custodyStatus) { this.custodyStatus = custodyStatus; }
    public String getSecretQuestion() { return secretQuestion; }
    public void setSecretQuestion(String secretQuestion) { this.secretQuestion = secretQuestion; }
    public String getReward() { return reward; }
    public void setReward(String reward) { this.reward = reward; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public MultipartFile getImage() { return image; }
    public void setImage(MultipartFile image) { this.image = image; }
}
