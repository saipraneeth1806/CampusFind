package com.example.campusfind.dto;

import com.example.campusfind.model.Report;
import java.util.List;

public record MatchCandidate(Report report, int score, long daysApart, List<String> reasons) { }
